package com.martin.intellij.plugin.mockbuilder.action

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.psi.PsiJavaFile
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent
import com.martin.intellij.plugin.mockbuilder.dialog.CreateMockBuilderDialog
import com.martin.intellij.plugin.mockbuilder.util.PsiUtils

class MockBuilderGeneratorActionHandler : EditorActionHandler()
{
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?)
    {
        val project = editor.project ?: return
        val subjectFile = dataContext?.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val subjectClass = subjectFile.classes.getOrNull(0) ?: return

        val createMockBuilderDialog = CreateMockBuilderDialog(project, subjectFile.packageName)
        createMockBuilderDialog.show()

        val packageName = createMockBuilderDialog.targetName
        val psiDirectory = PsiUtils.createDirectoryByPackageName(subjectFile, project, packageName)

        val mockBuilderClass = MyWriteAction(project, subjectFile)
        {
            project.getComponent(MockBuilderGeneratorProjectComponent::class.java).execute(subjectClass, psiDirectory)
        }.perform()

        mockBuilderClass.navigate(true)
    }
}
