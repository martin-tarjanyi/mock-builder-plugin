package com.martin.intellij.plugin.unittest.action

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.psi.PsiJavaFile
import com.martin.intellij.plugin.common.dialog.PackageDestinationDialog
import com.martin.intellij.plugin.common.util.PsiUtils
import com.martin.intellij.plugin.mockbuilder.action.MyWriteAction
import com.martin.intellij.plugin.unittest.component.UnitTestGeneratorProjectComponent

class UnitTestGeneratorAction : EditorAction(
        UnitTestGeneratorActionHandler())

class UnitTestGeneratorActionHandler : EditorActionHandler()
{
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?)
    {
        val project = editor.project ?: return
        val subjectFile = dataContext?.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val subjectClass = subjectFile.classes.getOrNull(0) ?: return

        val unitTestDialog = PackageDestinationDialog(project, subjectFile.packageName)
        unitTestDialog.show()

        val packageName = unitTestDialog.targetName
        val psiDirectory = PsiUtils.createDirectoryByPackageName(subjectFile, project, packageName)

        val unitTestClass = MyWriteAction(project, subjectFile) {
            project.getComponent(UnitTestGeneratorProjectComponent::class.java).execute(subjectClass, psiDirectory)
        }.perform()

        unitTestClass.navigate(true)
    }
}
