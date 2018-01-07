package com.martin.intellij.plugin.mockbuilder.component.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent
import com.martin.intellij.plugin.mockbuilder.extension.createStatementFromText
import com.martin.intellij.plugin.mockbuilder.extension.lastCharIfDigit

class MockBuilderGeneratorProjectComponentImpl(project: Project) : MockBuilderGeneratorProjectComponent
{
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)
    private val elementFactory = JavaPsiFacade.getElementFactory(project)
    private val javaDirectoryService = JavaDirectoryService.getInstance()

    override fun execute(originalJavaFile: PsiJavaFile): PsiClass
    {
        val originalDirectory = originalJavaFile.containingDirectory
        val originalClass = originalJavaFile.classes[0]

        val mockBuilderClass = javaDirectoryService.createClass(originalDirectory, "${originalClass.name}MockBuilder")
        val mockBuilderJavaFile = mockBuilderClass.containingFile as PsiJavaFile

        val methodsToMock = originalClass.allMethods
                .asSequence()
                .filter { isNotMethodInheritedFromObject(it)}
                .filter { isPublic(it) }
                .toList()

        val uniqueStubFields = generateStubReturnedFieldsForMockedMethods(methodsToMock)

        mockBuilderJavaFile.importList?.apply {
            addEasyMockStaticImport()
        }

        mockBuilderClass.apply {
            addFieldsForMockedMethodsReturnValues(uniqueStubFields)
            addEmptyPrivateConstructor()
            addStaticBuilderFactoryMethod(originalClass, mockBuilderClass)
            addWithMethods(mockBuilderClass)
            addBuildMethod(originalClass, methodsToMock, uniqueStubFields)
        }

        return mockBuilderClass
    }

    private fun PsiClass.addBuildMethod(originalClass: PsiClass, methodsToMock: List<PsiMethod>,
                                        uniqueStubFields: List<PsiField>)
    {
        add(elementFactory.createMethod("build", elementFactory.createType(originalClass)).apply {
            body?.apply {
                val mockVariableName = originalClass.name!!.decapitalize()

                add(elementFactory.createStatementFromText(
                        "${originalClass.name} $mockVariableName = createMock(${originalClass.name}.class);"))

                methodsToMock.filter { isNotVoid(it) }.forEachIndexed { index, method ->
                    val parameters = method.parameterList.parameters.map { it.type }.joinToString(separator = ", ") { mapToEasyMockType(it) }

                    add(elementFactory.createStatementFromText(
                            "expect($mockVariableName.${method.name}($parameters))" + ".andStubReturn(${uniqueStubFields[index].name});"))
                }

                add(elementFactory.createStatementFromText("replay($mockVariableName);"))

                add(elementFactory.createStatementFromText("return $mockVariableName;"))

            }
        })
    }

    private fun PsiClass.addWithMethods(mockBuilderClass: PsiClass)
    {
        allFields.forEach { field ->
            add(elementFactory.createMethod("with${field.name.capitalize()}",
                    elementFactory.createType(mockBuilderClass)).apply {
                parameterList.add(elementFactory.createParameter(field.name, field.type))

                body?.apply {
                    add(elementFactory.createStatementFromText("this.${field.name} = ${field.name};"))
                    add(elementFactory.createStatementFromText("return this;"))
                }
            })
        }
    }

    private fun PsiClass.addStaticBuilderFactoryMethod(originalClass: PsiClass, mockBuilderClass: PsiClass)
    {
        add(elementFactory.createMethod("a${originalClass.name}", elementFactory.createType(mockBuilderClass)).apply {
            modifierList.setModifierProperty(PsiModifier.PUBLIC, true)
            body?.add(elementFactory.createStatementFromText("return new ${mockBuilderClass.name}();", null))
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
        val fieldNames = mutableSetOf<String>()

        return methodsToMock.asSequence().filter { isNotVoid(it) }.map {
            val generatedFieldName = generateStubFieldName(it)
            if (fieldNames.add(generatedFieldName))
            {
                elementFactory.createField(generatedFieldName, it.returnType!!)
            } else
            {
                val uniqueDigit = generatedFieldName.lastCharIfDigit()?.plus(1)?.toString() ?: "2"
                val uniqueName = "$generatedFieldName$uniqueDigit"
                fieldNames.add(uniqueName)
                elementFactory.createField(uniqueName, it.returnType!!)
            }
        }.toList()
    }

    private fun generateStubFieldName(it: PsiMethod) = (it.returnType as PsiClassReferenceType).className.decapitalize()

    private fun isNotVoid(it: PsiMethod) = it.returnType?.takeIf { it != PsiType.VOID } != null

    private fun isPublic(it: PsiMethod) = it.modifierList.hasModifierProperty(PsiModifier.PUBLIC)

    private fun isNotMethodInheritedFromObject(
            it: PsiMethod) = it.containingClass?.name?.takeIf { it != "Object" } != null
}
