package com.martin.intellij.plugin

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.PsiClassReferenceType

class MockBuilderGeneratorActionHandler : EditorWriteActionHandler()
{
    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext)
    {
        if (!isApplicable(editor, dataContext)) return

        val project = editor.project!!
        val psiJavaFile = dataContext.getData(CommonDataKeys.PSI_FILE.name) as PsiJavaFile

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val javaDirectoryService = JavaDirectoryService.getInstance()

        val containingDirectory = psiJavaFile.containingDirectory

        val originalClass = psiJavaFile.classes.firstOrNull() ?: return

        val mockBuilderClass = javaDirectoryService.createClass(containingDirectory, "${originalClass.name}MockBuilder")

        originalClass.allMethods.asSequence()
                .filter { isNotMethodInheritedFromObject(it) }
                .filter { isPublic(it) }
                .filter { isNotVoid(it) }
                .map { elementFactory.createField(generateStubFieldName(it), it.returnType!!) }
                .forEach {
                    mockBuilderClass.add(it)
                }

        mockBuilderClass.add(elementFactory.createConstructor().apply {
            modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
        })

        mockBuilderClass.add(
                elementFactory.createMethod("a${originalClass.name}", elementFactory.createType(mockBuilderClass))
                        .apply {
                            modifierList.setModifierProperty(PsiModifier.PUBLIC, true)
                            body?.add(elementFactory.createStatementFromText("return new ${mockBuilderClass.name}();", null))
                        })


        CodeStyleManager.getInstance(project).reformat(mockBuilderClass)

        Messages.showMessageDialog("done", "My Message Box", Messages.getInformationIcon())
    }

    private fun generateStubFieldName(it: PsiMethod) = (it.returnType as PsiClassReferenceType).className.decapitalize()

    private fun isNotVoid(it: PsiMethod) = it.returnType?.takeIf { it != PsiType.VOID } != null

    private fun isPublic(it: PsiMethod) = it.modifierList.hasModifierProperty(PsiModifier.PUBLIC)

    private fun isNotMethodInheritedFromObject(it: PsiMethod) = it.containingClass?.name?.takeIf { it != "Object" } != null

    private fun isApplicable(editor: Editor, dataContext: DataContext): Boolean
    {
        val isProjectOpen = editor.project != null
        val isJavaFile = dataContext.getData(CommonDataKeys.PSI_FILE.name) as? PsiJavaFile != null

        return isProjectOpen && isJavaFile
    }
}



//        val createTypeFromText = elementFactory.createTypeFromText(CommonClassNames.JAVA_LANG_STRING, null)

//var testDirectoryName = containingDirectory.name.replace("main", "test")
//
//val module = ModuleUtil.findModuleForFile(psiJavaFile.virtualFile, project)!!
