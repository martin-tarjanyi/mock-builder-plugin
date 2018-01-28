package com.martin.intellij.plugin.unittest.component.impl

import com.intellij.lang.java.JavaImportOptimizer
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTypesUtil
import com.martin.intellij.plugin.common.util.createPrivateMethod
import com.martin.intellij.plugin.common.util.createStatementFromText
import com.martin.intellij.plugin.common.util.findIndefiniteArticle
import com.martin.intellij.plugin.common.util.isPublic
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent
import com.martin.intellij.plugin.unittest.component.TestCaseResourceMapperComponent
import com.martin.intellij.plugin.unittest.component.UnitTestGeneratorProjectComponent

class UnitTestGeneratorProjectComponentImpl(
    private val project: Project,
    private val mockBuilderGenerator: MockBuilderGeneratorProjectComponent,
    private val testCaseResourceMapper: TestCaseResourceMapperComponent,
    private val elementFactory: PsiElementFactory,
    private val javaPsiFacade: JavaPsiFacade,
    private val psiShortNamesCache: PsiShortNamesCache,
    private val codeStyleManager: CodeStyleManager,
    private val javaDirectoryService: JavaDirectoryService
) : UnitTestGeneratorProjectComponent
{
    private val javaImportOptimizer = JavaImportOptimizer()

    override fun execute(
        subjectClass: PsiClass,
        unitTestPsiDirectory: PsiDirectory,
        mockBuilderPsiDirectory: PsiDirectory
    ): PsiClass
    {
        val testClassName = "${subjectClass.name}Test"

        val foundUnitTestClasses =
            psiShortNamesCache.getClassesByName(testClassName, GlobalSearchScope.allScope(project))

        if (foundUnitTestClasses.isNotEmpty())
        {
            return foundUnitTestClasses.first()
        }

        val unitTestClass = javaDirectoryService.createClass(unitTestPsiDirectory, testClassName)

        val primaryConstructor = subjectClass.allMethods.filter { it.isConstructor }.maxBy { it.parameters.size }
                ?: throw IllegalStateException(
                    "Subject class does not have any constructor."
                )

        val constructorParameters = primaryConstructor.parameterList.parameters.toList()

        val mockedFields = constructorParameters.map { (elementFactory.createField(it.name!!, it.type)) }

        val mocks = constructorParameters.associateBy({ param: PsiParameter -> param.name!! }, { param: PsiParameter ->
            val paramClass = PsiTypesUtil.getPsiClass(param.type)!!
            mockBuilderGenerator.execute(paramClass, mockBuilderPsiDirectory)
        })

        val givenStepsForMockedDependencies = createGivenStepForMockedDependencies(mocks)

        val methodsToTest =
            subjectClass.allMethods.filter { it.isPublic() and !it.isConstructor and (it.containingClass?.name == subjectClass.name) }

        val testCasesResources = methodsToTest.map {
            testCaseResourceMapper.map(it, givenStepsForMockedDependencies, mockedFields, subjectClass)
        }
        unitTestClass.apply {
            mockedFields.forEach { add(it) }
            testCasesResources.flatMap { it.parameterFields }.distinctBy { it.name + it.type }.forEach { add(it) }
            testCasesResources.mapNotNull { it.actualField }.distinctBy { it.name + it.type }.forEach { add(it) }
            testCasesResources.map { it.testCaseMethod }.onEach { add(it) }
            testCasesResources.flatMap { it.givenMethods }.distinctBy { it.name + it.returnType }.forEach { add(it) }
            testCasesResources.map { it.whenMethod }.forEach { add(it) }
            testCasesResources.mapNotNull { it.thenMethod }.forEach { add(it) }

            modifierList?.addAnnotation("Test")?.setDeclaredAttributeValue(
                "groups", elementFactory.createExpressionFromText("\"unit\"", null)
            )
        }

        (unitTestClass.containingFile as PsiJavaFile).importList?.apply {
            mocks.values.forEach { add(elementFactory.createImportStatement(it)) }
            addTestNgImports()
        }

        javaImportOptimizer.processFile(unitTestClass.containingFile).run()
        codeStyleManager.reformat(unitTestClass)

        return unitTestClass
    }

    private fun createGivenStepForMockedDependencies(mocks: Map<String, PsiClass>): List<PsiMethod>
    {
        return mocks.map { (parameterName, mockClass) ->
            val indefiniteArticle = parameterName.findIndefiniteArticle().capitalize()
            elementFactory.createPrivateMethod("given$indefiniteArticle${parameterName.capitalize()}", PsiType.VOID)
                .apply {
                    body?.apply {
                        val factoryMethod = mockClass.allMethods.find {
                            it.modifierList.hasModifierProperty(PsiModifier.STATIC)
                        }
                        add(elementFactory.createStatementFromText("this.$parameterName = ${mockClass.name}.${factoryMethod!!.name}().build();"))
                    }
                }
        }
    }

    private fun PsiImportList.addTestNgImports()
    {
        val testNgAssertClass = javaPsiFacade.findClass(
            "org.testng.Assert", GlobalSearchScope.allScope(project)
        ) ?: throw IllegalStateException("TestNG is not on classpath.")

        val testAnnotation = javaPsiFacade.findClass(
            "org.testng.annotations.Test", GlobalSearchScope.allScope(project)
        ) ?: throw IllegalStateException("TestNG is not on classpath.")

        add(elementFactory.createImportStaticStatement(testNgAssertClass, "*"))
        add(elementFactory.createImportStatement(testAnnotation))
    }
}
