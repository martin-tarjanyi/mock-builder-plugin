package com.martin.intellij.plugin.mockbuilder.component.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent
import com.martin.intellij.plugin.mockbuilder.extension.addOccurence
import com.martin.intellij.plugin.mockbuilder.extension.createStatementFromText
import com.martin.intellij.plugin.mockbuilder.extension.findIndefiniteArticle

class MockBuilderGeneratorProjectComponentImpl(project: Project) : MockBuilderGeneratorProjectComponent
{
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)
    private val elementFactory = JavaPsiFacade.getElementFactory(project)
    private val javaDirectoryService = JavaDirectoryService.getInstance()

    override fun execute(originalJavaFile: PsiJavaFile, psiDirectory: PsiDirectory?): PsiClass
    {
        val originalClass = originalJavaFile.classes[0]

        val mockBuilderClass = javaDirectoryService.createClass(psiDirectory!!, "${originalClass.name}MockBuilder")
        val mockBuilderJavaFile = mockBuilderClass.containingFile as PsiJavaFile

        val methodsToMock = originalClass.allMethods
                .asSequence()
                .filter { isNotMethodInheritedFromObject(it)}
                .filter { isPublic(it) }
                .toList()

        val uniqueStubFields = generateStubReturnedFieldsForMockedMethods(methodsToMock)

        mockBuilderClass.apply {
            addFieldsForMockedMethodsReturnValues(uniqueStubFields)
            addEmptyPrivateConstructor()
            addStaticBuilderFactoryMethod(originalClass)
            addWithMethods()
            addBuildMethod(originalClass, methodsToMock, uniqueStubFields)
        }

        mockBuilderJavaFile.importList?.apply {
            addEasyMockStaticImport()
        }

        return mockBuilderClass
    }

    private fun PsiClass.addBuildMethod(originalClass: PsiClass, methodsToMock: List<PsiMethod>,
                                        uniqueStubFields: List<PsiField>)
    {
        add(elementFactory.createMethod("build", elementFactory.createType(originalClass)).apply {
            body?.apply {
                val mockVariableName = originalClass.name!!.decapitalize()

                addCreateMockStatement(originalClass, mockVariableName)
                addExpectStatementsForMethodsWithReturnValue(methodsToMock, mockVariableName, uniqueStubFields)
                addReplayStatement(mockVariableName)
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

    private fun PsiCodeBlock.addExpectStatementsForMethodsWithReturnValue(methodsToMock: List<PsiMethod>,
                                                                          mockVariableName: String,
                                                                          uniqueStubFields: List<PsiField>)
    {
        val psiJavaFile = containingFile as PsiJavaFile

        methodsToMock.filter { isNotVoid(it) }.forEachIndexed { index, method ->
            val parameters = method.parameterList.parameters
                    .map { it.type }
                    .onEach { PsiTypesUtil.getPsiClass(it)?.also { psiJavaFile.importList?.add(elementFactory.createImportStatement(it)) } }
                    .joinToString(separator = ", ") { mapToEasyMockType(it) }

            add(elementFactory.createStatementFromText(
                    "expect($mockVariableName.${method.name}($parameters))" + ".andStubReturn(${uniqueStubFields[index].name});"))
        }
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
            add(elementFactory.createMethod("with${field.name.capitalize()}", elementFactory.createType(psiClass)).apply {
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

        add(elementFactory.createMethod("$indefiniteArticle${originalClass.name}", elementFactory.createType(psiClass)).apply {
            modifierList.setModifierProperty(PsiModifier.PUBLIC, true)
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
        val easymockClass = javaPsiFacade.findClass("org.easymock.EasyMock", GlobalSearchScope.allScope(project)) ?: throw IllegalStateException("EasyMock is not on classpath.")

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
            else ->
            {
                throw UnsupportedOperationException(
                        "Unsupported parameter type in class to mock. " + "Only primitive and reference types are supported.")

            }
        }
    }


    private fun generateStubReturnedFieldsForMockedMethods(methodsToMock: List<PsiMethod>): List<PsiField>
    {
        val fieldNamesWithOccurrences = mutableMapOf<String, Int>()

        return methodsToMock.asSequence().filter { isNotVoid(it) }.map {
            val generatedFieldNameFromType = generateNameFromType(it)
            val cardinality = fieldNamesWithOccurrences.addOccurence(generatedFieldNameFromType)

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

    private fun generateNameFromType(it: PsiMethod): String
    {
        val returnType = it.returnType

        return when (returnType)
        {
            is PsiClassReferenceType -> returnType.className.decapitalize()
            is PsiPrimitiveType -> returnType.let { mapPrimitive(it) }
//            is PsiArrayType -> returnType.let { mapPrimitive(it.) }
            else -> throw RuntimeException("Unexpected type.")
        }
    }

    private fun mapPrimitive(primitiveType: PsiPrimitiveType): String = when (primitiveType.name)
    {
        "int", "long", "short" -> "number"
        else -> "primitive"
    }

    private fun isNotVoid(it: PsiMethod) = it.returnType?.takeIf { it != PsiType.VOID } != null

    private fun isPublic(it: PsiMethod) = it.modifierList.hasModifierProperty(PsiModifier.PUBLIC)

    private fun isNotMethodInheritedFromObject(
            it: PsiMethod) = it.containingClass?.name?.takeIf { it != "Object" } != null
}
