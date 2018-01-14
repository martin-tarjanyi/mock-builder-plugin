package com.martin.intellij.plugin.unittest.component.impl

import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import com.martin.intellij.plugin.common.util.createPrivateMethod
import com.martin.intellij.plugin.common.util.createStatementFromText
import com.martin.intellij.plugin.common.util.findIndefiniteArticle
import com.martin.intellij.plugin.common.util.generateName
import com.martin.intellij.plugin.unittest.component.TestCaseResourceMapperComponent
import com.martin.intellij.plugin.unittest.model.TestCaseResources

class TestCaseResourceMapperComponentImpl(private val elementFactory: PsiElementFactory,
                                          private val psiManager: PsiManager) : TestCaseResourceMapperComponent
{
    override fun map(methodToTest: PsiMethod, givenStepsForMocks: List<PsiMethod>, mockedFields: List<PsiField>,
                     subjectClass: PsiClass): TestCaseResources
    {
        val parameters = methodToTest.parameterList.parameters.toList()
        val parameterFields = parameters.map { elementFactory.createField(it.name!!, it.type) }

        val actualField = createFieldForActualResult(methodToTest)

        val givenStepsForParameters = createGivenStepsForMethodParameters(parameters)

        val allGivenSteps = givenStepsForParameters + givenStepsForMocks

        val whenMethod = createWhenMethod(methodToTest, mockedFields, subjectClass)

        val thenMethod = createThenMethod(methodToTest)

        val testCaseMethod = createTestCaseMethod(methodToTest, givenStepsForParameters, givenStepsForMocks, whenMethod,
                thenMethod)

        return TestCaseResources(parameterFields = parameterFields, actualField = actualField,
                givenMethods = allGivenSteps, whenMethod = whenMethod, thenMethod = thenMethod,
                testCaseMethod = testCaseMethod)
    }

    private fun createFieldForActualResult(methodToTest: PsiMethod): PsiField?
    {
        return methodToTest.returnType?.takeIf { it != PsiType.VOID }?.let {
            elementFactory.createField("actual${it.generateName().capitalize()}", it)
        }
    }

    private fun createGivenStepsForMethodParameters(parametersForMethods: List<PsiParameter>): List<PsiMethod>
    {
        return parametersForMethods.map { parameter ->
            val parameterName = parameter.name ?: throw IllegalStateException(
                    "Constructor parameter should have a name.")
            val indefiniteArticle = parameterName.findIndefiniteArticle().capitalize()

            elementFactory.createPrivateMethod("given$indefiniteArticle${parameterName.capitalize()}",
                    PsiType.VOID).apply {
                parameterList.apply {
                    add(elementFactory.createParameter(parameterName, parameter.type))
                }
                body?.apply {
                    add(elementFactory.createStatementFromText("this.$parameterName = $parameterName;"))
                }
            }
        }
    }

    private fun createWhenMethod(methodToTest: PsiMethod, mockedFields: List<PsiField>,
                                 subjectClass: PsiClass): PsiMethod
    {
        return elementFactory.createPrivateMethod("when${methodToTest.name.capitalize()}IsCalled", PsiType.VOID).apply {
            body?.apply {
                val parametersForConstructorInvocation = mockedFields.joinToString { it.name }
                val methodParameters = methodToTest.parameterList.parameters.map { it.name }.joinToString()
                val returnType = methodToTest.returnType

                if (returnType == null || returnType == PsiType.VOID)
                {
                    add(elementFactory.createStatementFromText(
                            "new ${subjectClass.name}($parametersForConstructorInvocation).${methodToTest.name}($methodParameters);"))
                } else
                {
                    val generatedNameForReturnValue = methodToTest.returnType!!.generateName().capitalize()
                    add(elementFactory.createStatementFromText(
                            "this.actual$generatedNameForReturnValue = new ${subjectClass.name}($parametersForConstructorInvocation)" + ".${methodToTest.name}($methodParameters);"))

                }
            }
        }
    }

    private fun createThenMethod(methodToTest: PsiMethod): PsiMethod?
    {
        return methodToTest.takeIf { it.returnType != PsiType.VOID }?.let {
            val methodReturnValueName = it.returnType!!.generateName()
            elementFactory.createPrivateMethod("then${methodReturnValueName.capitalize()}IsCorrect",
                    PsiType.VOID).apply {
                val expectedParameterName = "expected${methodReturnValueName.capitalize()}"
                val actualFieldName = "actual${methodReturnValueName.capitalize()}"
                val testCaseNameVariableName = "testCaseName"
                parameterList.apply {
                    add(elementFactory.createParameter(expectedParameterName, it.returnType!!))
                    add(elementFactory.createParameter(testCaseNameVariableName,
                            PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project))))
                }
                body?.apply {
                    add(elementFactory.createStatementFromText(
                            "assertEquals($actualFieldName, $expectedParameterName, $testCaseNameVariableName);"))

                }
            }
        }
    }

    private fun createTestCaseMethod(methodToTest: PsiMethod, givenStepsForParameters: List<PsiMethod>,
                                     givenStepsForMocks: List<PsiMethod>, whenMethod: PsiMethod,
                                     thenMethod: PsiMethod?): PsiMethod
    {
        val defaultValueForReturnType = methodToTest.returnType?.let { PsiTypesUtil.getDefaultValueOfType(it) }

        return elementFactory.createMethod("should${methodToTest.name.capitalize()}Correctly", PsiType.VOID).apply {
            body?.apply {
                givenStepsForParameters.forEach { givenStep ->
                    val defaultValueForParameterType = PsiTypesUtil.getDefaultValueOfType(
                            givenStep.parameterList.parameters.first().type)

                    add(elementFactory.createStatementFromText("${givenStep.name}($defaultValueForParameterType);"))
                }

                givenStepsForMocks.forEach { givenStep ->
                    add(elementFactory.createStatementFromText("${givenStep.name}();"))
                }

                add(elementFactory.createStatementFromText("${whenMethod.name}();"))

                thenMethod?.also { add(elementFactory.createStatementFromText(
                        "${it.name}($defaultValueForReturnType, \"Test Case Name\");"))
                }
            }
            modifierList.addAnnotation("Test")
        }
    }

}
