package com.martin.intellij.plugin.mockbuilder.component.impl

import com.intellij.lang.java.JavaImportOptimizer
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTypesUtil
import com.martin.intellij.plugin.common.util.*
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent

class MockBuilderGeneratorProjectComponentImpl(private val project: Project,
                                               private val elementFactory: PsiElementFactory,
                                               private val javaPsiFacade: JavaPsiFacade,
                                               private val psiShortNamesCache: PsiShortNamesCache,
                                               private val psiParserFacade: PsiParserFacade,
                                               private val codeStyleManager: CodeStyleManager,
                                               private val javaDirectoryService: JavaDirectoryService) : MockBuilderGeneratorProjectComponent
{
    private val javaImportOptimizer = JavaImportOptimizer()

    override fun execute(subjectClass: PsiClass, psiDirectory: PsiDirectory): PsiClass
    {
        val mockBuilderClassName = "${subjectClass.name}MockBuilder"

        val foundMockBuilderClass = psiShortNamesCache.getClassesByName(mockBuilderClassName, GlobalSearchScope.allScope(project))

        if (foundMockBuilderClass.isNotEmpty())
        {
            return foundMockBuilderClass.first()
        }

        val mockBuilderClass = javaDirectoryService.createClass(psiDirectory, mockBuilderClassName)
        val mockBuilderJavaFile = mockBuilderClass.containingFile as PsiJavaFile

        val methodsToMock = subjectClass.allMethods.asSequence().filter { it.isPublic() and !it.isConstructor and (it.containingClass?.name == subjectClass.name) }.toList()

        val uniqueStubFields = generateStubReturnedFieldsForMockedMethods(methodsToMock)

        mockBuilderClass.apply {
            addFieldsForMockedMethodsReturnValues(uniqueStubFields)
            addEmptyPrivateConstructor()
            addStaticBuilderFactoryMethod(subjectClass)
            addWithMethods()
            addBuildMethod(subjectClass, methodsToMock, uniqueStubFields)
        }

        mockBuilderJavaFile.importList?.apply {
            addEasyMockStaticImport()
        }

        javaImportOptimizer.processFile(mockBuilderJavaFile)
        codeStyleManager.reformat(mockBuilderClass)

        return mockBuilderClass
    }

    private fun PsiClass.addBuildMethod(subjectClass: PsiClass, methodsToMock: List<PsiMethod>,
                                        uniqueStubFields: List<PsiField>)
    {
        add(elementFactory.createMethod("build", elementFactory.createType(subjectClass)).apply {
            body?.apply {
                val mockVariableName = subjectClass.name?.decapitalize() ?: throw IllegalStateException(
                        "Subject class has no name.")

                addCreateMockStatement(subjectClass, mockVariableName)
                add(psiParserFacade.doubleLineBreak())
                addExpectStatementsForMethods(methodsToMock, mockVariableName, uniqueStubFields)
                add(psiParserFacade.doubleLineBreak())
                addReplayStatement(mockVariableName)
                add(psiParserFacade.doubleLineBreak())
                addReturnStatement(mockVariableName)
            }
        })
    }

    private fun PsiCodeBlock.addReturnStatement(mockVariableName: String)
    {
        add(elementFactory.createStatementFromText("return $mockVariableName;"))
    }

    private fun PsiCodeBlock.addReplayStatement(mockVariableName: String)
    {
        add(elementFactory.createStatementFromText("replay($mockVariableName);"))
    }

    private fun PsiCodeBlock.addExpectStatementsForMethods(methodsToMock: List<PsiMethod>,
                                                           mockVariableName: String,
                                                           uniqueStubFields: List<PsiField>)
    {
        val psiJavaFile = containingFile as PsiJavaFile

        methodsToMock.filter { !isVoid(it) }.forEachIndexed { index, method ->
            val parameters = createMethodParametersForEasyMockApi(method, psiJavaFile)

            add(elementFactory.createStatementFromText(
                    "expect($mockVariableName.${method.name}($parameters))" + ".andStubReturn(${uniqueStubFields[index].name});"))
        }

        methodsToMock.filter { isVoid(it) }
                .takeIf { it.isNotEmpty() }
                ?.also { add(psiParserFacade.doubleLineBreak()) }
                ?.forEach { val parameters = createMethodParametersForEasyMockApi(it, psiJavaFile)
                    add(elementFactory.createStatementFromText("$mockVariableName.${it.name}($parameters);"))
                }

    }

    private fun createMethodParametersForEasyMockApi(method: PsiMethod, psiJavaFile: PsiJavaFile): String
    {
        return method.parameterList.parameters.map { it.type }.onEach {
            PsiTypesUtil.getPsiClass(it)?.also { psiJavaFile.importList?.add(elementFactory.createImportStatement(it)) }
        }.joinToString(separator = ", ") { mapToEasyMockType(it) }
    }

    private fun PsiCodeBlock.addCreateMockStatement(originalClass: PsiClass, mockVariableName: String)
    {
        add(elementFactory.createStatementFromText(
                "${originalClass.name} $mockVariableName = createMock(${originalClass.name}.class);"))

        val psiJavaFile = containingFile as PsiJavaFile
        psiJavaFile.importList?.add(elementFactory.createImportStatement(originalClass))
    }

    private fun PsiClass.addWithMethods()
    {
        val psiClass = this
        allFields.forEach { field ->
            add(elementFactory.createMethod("with${field.name.capitalize()}",
                    elementFactory.createType(psiClass)).apply {
                parameterList.add(elementFactory.createParameter(field.name, field.type))

                body?.apply {
                    add(elementFactory.createStatementFromText("this.${field.name} = ${field.name};"))
                    add(elementFactory.createStatementFromText("return this;"))
                }
            })
        }
    }

    private fun PsiClass.addStaticBuilderFactoryMethod(originalClass: PsiClass)
    {
        val psiClass = this
        val indefiniteArticle = originalClass.name!!.findIndefiniteArticle()

        add(elementFactory.createMethod("$indefiniteArticle${originalClass.name}",
                elementFactory.createType(psiClass)).apply {
            modifierList.setModifierProperty(PsiModifier.PUBLIC, true)
            modifierList.setModifierProperty(PsiModifier.STATIC, true)
            body?.add(elementFactory.createStatementFromText("return new ${psiClass.name}();", null))
        })
    }

    private fun PsiClass.addEmptyPrivateConstructor()
    {
        add(elementFactory.createConstructor().apply {
            modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
        })
    }

    private fun PsiClass.addFieldsForMockedMethodsReturnValues(uniqueStubFields: List<PsiField>)
    {
        uniqueStubFields.forEach {
            add(it)
            val psiClass = PsiTypesUtil.getPsiClass(it.type)
            val psiJavaFile = it.containingFile as PsiJavaFile
            if (psiClass != null)
            {
                psiJavaFile.importList?.add(elementFactory.createImportStatement(psiClass))
            }
        }
    }

    private fun PsiImportList.addEasyMockStaticImport()
    {
        val easymockClass = javaPsiFacade.findClass("org.easymock.EasyMock",
                GlobalSearchScope.allScope(project)) ?: throw IllegalStateException("EasyMock is not on classpath.")

        add(elementFactory.createImportStaticStatement(easymockClass, "*"))
    }

    private fun mapToEasyMockType(psiType: PsiType): String
    {
        when (psiType)
        {
            is PsiClassReferenceType ->
            {
                return "isA(${psiType.className}.class)"
            }
            is PsiPrimitiveType ->
            {
                return "any${psiType.name.capitalize()}()"
            }
            is PsiEllipsisType ->
            {
                return "isA(${psiType.presentableText.removeSuffix("...")}.class)"
            }
            else ->
            {
                return "isA(${psiType.presentableText}.class)"
            }
        }
    }

    private fun generateStubReturnedFieldsForMockedMethods(methodsToMock: List<PsiMethod>): List<PsiField>
    {
        val fieldNamesWithOccurrences = mutableMapOf<String, Int>()

        return methodsToMock.asSequence().filter { !isVoid(it) }.map {
            val generatedFieldNameFromType = it.returnType!!.generateName()
            val cardinality = fieldNamesWithOccurrences.addOccurrence(generatedFieldNameFromType)

            if (cardinality == 1)
            {
                elementFactory.createField(generatedFieldNameFromType, it.returnType!!)
            } else
            {
                val uniqueName = "$generatedFieldNameFromType$cardinality"
                elementFactory.createField(uniqueName, it.returnType!!)
            }
        }.toList()
    }

    private fun isVoid(psiMethod: PsiMethod) = psiMethod.returnType == PsiType.VOID
}
