package com.martin.intellij.plugin.common.dialog

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ReferenceEditorComboWithBrowseButton
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke

class PackageDestinationDialog(project: Project, packageName: String) : DialogWrapper(project)
{
    companion object
    {
        private val RECENTS_KEY = "CreateMockBuilderDialog.RecentsKey"
    }

    private val targetPackageField = ReferenceEditorComboWithBrowseButton(null, packageName, project, true, RECENTS_KEY)

    var targetName: String = ""

    init
    {
        super.init()
        title = "Create Mock Builder"

        targetPackageField.addActionListener(ChooserDisplayerActionListener(project, targetPackageField))
    }

    override fun createCenterPanel(): JComponent?
    {
        val panel = JPanel(GridBagLayout())
        val gridBagConstraints = GridBagConstraints()

        panel.border = IdeBorderFactory.createBorder()

        gridBagConstraints.insets = Insets(4, 8, 4, 8)
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 3
        gridBagConstraints.weightx = 0.0
        gridBagConstraints.gridwidth = 1
        panel.add(JLabel(CodeInsightBundle.message("dialog.create.class.destination.package.label")), gridBagConstraints)

        gridBagConstraints.gridx = 1
        gridBagConstraints.weightx = 1.0

        val clickAction = object : AnAction()
        {
            override fun actionPerformed(e: AnActionEvent)
            {
                targetPackageField.button.doClick()
            }
        }

        clickAction.registerCustomShortcutSet(
                CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)),
                targetPackageField.childComponent)

        addInnerPanelForDestinationPackageField(panel, gridBagConstraints)

        return panel
    }

    override fun doOKAction()
    {
        targetName = targetPackageField.text
        super.doOKAction()
    }

    private fun addInnerPanelForDestinationPackageField(panel: JPanel, gbConstraints: GridBagConstraints)
    {
        val innerPanel = createInnerPanelForDestinationPackageField()
        panel.add(innerPanel, gbConstraints)
    }

    private fun createInnerPanelForDestinationPackageField(): JPanel
    {
        val innerPanel = JPanel(BorderLayout())
        innerPanel.add(targetPackageField, BorderLayout.CENTER)
        return innerPanel
    }
}
