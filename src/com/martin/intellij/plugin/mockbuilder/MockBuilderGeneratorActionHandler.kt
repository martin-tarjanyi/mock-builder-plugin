package com.martin.intellij.plugin.mockbuilder

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.lang.java.JavaImportOptimizer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope

class MockBuilderGeneratorActionHandler : CodeInsightActionHandler
{
    override fun invoke(project: Project, editor: Editor, originalFile: PsiFile)
    {
        if (originalFile !is PsiJavaFile) return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val javaDirectoryService = JavaDirectoryService.getInstance()

        val containingDirectory = originalFile.containingDirectory

        val originalClass = originalFile.classes.firstOrNull() ?: return

        val mockBuilderClass = javaDirectoryService.createClass(containingDirectory, "${originalClass.name}MockBuilder")

        val methodsToMock = originalClass.allMethods.asSequence()
                .filter { isNotMethodInheritedFromObject(it) }
                .filter { isPublic(it) }
                .toList()

        val uniqueStubFields = generateStubFields(methodsToMock, elementFactory)

        val mockBuilderJavaFile = mockBuilderClass.containingFile as PsiJavaFile

        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        mockBuilderJavaFile.importList?.apply {

            val easymockClass = javaPsiFacade.findClass("org.easymock.EasyMock",
                    GlobalSearchScope.allScope(project)) ?: throw IllegalStateException("EasyMock is not on classpath.")

            add(elementFactory.createImportStaticStatement(easymockClass, "*"))
            add(elementFactory.createImportStaticStatement(easymockClass, "expect"))
            add(elementFactory.createImportStaticStatement(easymockClass, "replay"))
            add(elementFactory.createImportStaticStatement(easymockClass, "isA"))
            add(elementFactory.createImportStaticStatement(easymockClass, "anyInt"))
            add(elementFactory.createImportStaticStatement(easymockClass, "anyBoolean"))
        }

        mockBuilderClass.apply {
            uniqueStubFields.forEach {
                add(it)
            }

            add(elementFactory.createConstructor().apply
            {
                modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
            })

            add(elementFactory.createMethod("a${originalClass.name}",
                    elementFactory.createType(mockBuilderClass)).apply {
                modifierList.setModifierProperty(PsiModifier.PUBLIC, true)
                body?.add(elementFactory.createStatementFromText("return new ${mockBuilderClass.name}();", null))
            })

            allFields.forEach { field ->
                add(elementFactory.createMethod("with${field.name.capitalize()}", elementFactory.createType(mockBuilderClass)).apply
                {
                    parameterList.add(elementFactory.createParameter(field.name, field.type))

                    body?.apply {
                        add(elementFactory.createStatementFromText("this.${field.name} = ${field.name};"))
                        add(elementFactory.createStatementFromText("return this;"))
                    }
                })
            }

            add(elementFactory.createMethod("build", elementFactory.createType(originalClass)).apply {
                body?.apply{
                    val mockVariableName = originalClass.name!!.decapitalize()

                    add(elementFactory.createStatementFromText(
                            "${originalClass.name} $mockVariableName = createMock(${originalClass.name}.class);"))

                    methodsToMock
                            .filter { isNotVoid(it) }
                            .forEachIndexed { index, method ->
                                val parameters = method.parameterList.parameters
                                        .map { it.type }
                                        .joinToString( separator = ", ") { mapToEasyMockType(it) }

                                add(elementFactory.createStatementFromText("expect($mockVariableName.${method.name}($parameters))" +
                                        ".andStubReturn(${uniqueStubFields[index].name});"))
                            }

                    add(elementFactory.createStatementFromText(
                            "replay($mockVariableName);"))

                    add(elementFactory.createStatementFromText(
                            "return $mockVariableName;"))

                }
            })
        }

        CodeStyleManager.getInstance(project).reformat(mockBuilderClass)

        JavaImportOptimizer().processFile(mockBuilderJavaFile).run()

        mockBuilderClass.navigate(true)
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
                        "Unsupported parameter type in class to mock. " +
                                "Only primitive and reference types are supported.")

            }
        }
    }


    private fun generateStubFields(methodsToMock: List<PsiMethod>, elementFactory: PsiElementFactory): List<PsiField>
    {
        val fieldNames = mutableSetOf<String>()

        return methodsToMock.asSequence()
                .filter { isNotVoid(it) }
                .map {
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


//        val createTypeFromText = elementFactory.createTypeFromText(CommonClassNames.JAVA_LANG_STRING, null)

//var testDirectoryName = containingDirectory.name.replace("main", "test")
//
//val module = ModuleUtil.findModuleForFile(psiJavaFile.virtualFile, project)!!


//Messages.showMessageDialog("done", "My Message Box", Messages.getInformationIcon())




//        val mockClassEditor = PsiUtilBase.findEditor(mockBuilderClass)
//
//        val mockClassContext = SimpleDataContext.getSimpleContext(CommonDataKeys.EDITOR.name, mockClassEditor,
//                dataContext)
//
////        psiJavaFile.virtualFile.move()
//
//        OptimizeImportsAction.actionPerformedImpl(mockClassContext)
