package com.martin.intellij.plugin.unittest.action

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.psi.PsiJavaFile
import com.martin.intellij.plugin.common.util.PsiUtils
import com.martin.intellij.plugin.mockbuilder.action.WriteActionWrapper
import com.martin.intellij.plugin.unittest.component.UnitTestGeneratorProjectComponent
import com.martin.intellij.plugin.unittest.dialog.UnitTestResourcesDestinationDialog

class UnitTestGeneratorAction : EditorAction(
        UnitTestGeneratorActionHandler())

class UnitTestGeneratorActionHandler : EditorActionHandler()
{
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?)
    {
        val project = editor.project ?: return
        val subjectFile = dataContext?.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val subjectClass = subjectFile.classes.getOrNull(0) ?: return

        val unitTestDialog = UnitTestResourcesDestinationDialog(project, subjectFile.packageName)
        unitTestDialog.show()

        val unitTestPsiDirectory = PsiUtils.createDirectoryByPackageName(subjectFile, project, unitTestDialog.unitTestTargetName)
        val mockBuilderPsiDirectory = PsiUtils.createDirectoryByPackageName(subjectFile, project, unitTestDialog.mockBuilderTargetName)

        val unitTestClass = WriteActionWrapper(project, subjectFile) {
            project.getComponent(UnitTestGeneratorProjectComponent::class.java).execute(subjectClass, unitTestPsiDirectory, mockBuilderPsiDirectory)
        }.perform()

        unitTestClass.navigate(true)
    }
}
