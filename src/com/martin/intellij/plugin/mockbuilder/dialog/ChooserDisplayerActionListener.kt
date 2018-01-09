package com.martin.intellij.plugin.mockbuilder.dialog

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.project.Project
import com.intellij.ui.ReferenceEditorComboWithBrowseButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class ChooserDisplayerActionListener(private val project: Project,
                                     private val comboWithBrowseButton: ReferenceEditorComboWithBrowseButton) : ActionListener
{
    override fun actionPerformed(e: ActionEvent?)
    {
        val chooser = PackageChooserDialog(CodeInsightBundle.message("dialog.create.class.package.chooser.title"), project)
        chooser.selectPackage(comboWithBrowseButton.text)
        chooser.show()

        comboWithBrowseButton.text = chooser.selectedPackage?.qualifiedName
    }
}
