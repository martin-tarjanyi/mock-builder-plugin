package com.martin.intellij.plugin.unittest.dialog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ReferenceEditorComboWithBrowseButton
import com.martin.intellij.plugin.common.dialog.ChooserDisplayerActionListener
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

class UnitTestResourcesDestinationDialog(project: Project, packageName: String) : DialogWrapper(project)
{
    companion object
    {
        private val UNIT_TEST_LOCATION_RECENTS_KEY = "CreateMockBuilderDialog.RecentsKey"
        private val MOCK_BUILDER_LOCATION_RECENTS_KEY = "CreateMockBuilderDialog.RecentsKey"
    }

    private val unitTestTargetPackageField = ReferenceEditorComboWithBrowseButton(
        null,
        packageName,
        project,
        true,
        UNIT_TEST_LOCATION_RECENTS_KEY
    )

    private val mockBuilderTargetPackageField = ReferenceEditorComboWithBrowseButton(
        null,
        packageName,
        project,
        true,
        MOCK_BUILDER_LOCATION_RECENTS_KEY
    )

    var unitTestTargetName: String = ""
    var mockBuilderTargetName: String = ""

    init
    {
        super.init()
        title = "Create Unit Test Resources"

        unitTestTargetPackageField.addActionListener(
            ChooserDisplayerActionListener(
                project, unitTestTargetPackageField
            )
        )
        mockBuilderTargetPackageField.addActionListener(
            ChooserDisplayerActionListener(
                project, mockBuilderTargetPackageField
            )
        )
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
        panel.add(JLabel("Unit Test Location"), gridBagConstraints)

        gridBagConstraints.gridx = 1
        gridBagConstraints.weightx = 1.0

        val unitTestClickAction = object : AnAction()
        {
            override fun actionPerformed(e: AnActionEvent)
            {
                unitTestTargetPackageField.button.doClick()
            }
        }

        unitTestClickAction.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyStroke.getKeyStroke(
                    KeyEvent.VK_ENTER,
                    InputEvent.SHIFT_DOWN_MASK
                )
            ),
                unitTestTargetPackageField.childComponent)

        addInnerPanelForDestinationPackageField(
            panel,
            gridBagConstraints,
            unitTestTargetPackageField
        )

        gridBagConstraints.insets = Insets(4, 8, 4, 8)
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 6
        gridBagConstraints.weightx = 0.0
        gridBagConstraints.gridwidth = 1
        panel.add(JLabel("Mock Builders Location"), gridBagConstraints)

        gridBagConstraints.gridx = 1
        gridBagConstraints.weightx = 1.0

        val clickAction = object : AnAction()
        {
            override fun actionPerformed(e: AnActionEvent)
            {
                mockBuilderTargetPackageField.button.doClick()
            }
        }

        clickAction.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyStroke.getKeyStroke(
                    KeyEvent.VK_ENTER,
                    InputEvent.SHIFT_DOWN_MASK
                )
            ),
            mockBuilderTargetPackageField.childComponent)

        addInnerPanelForDestinationPackageField(
            panel,
            gridBagConstraints,
            mockBuilderTargetPackageField
        )

        return panel
    }

    override fun doOKAction()
    {
        unitTestTargetName = unitTestTargetPackageField.text
        mockBuilderTargetName = mockBuilderTargetPackageField.text
        super.doOKAction()
    }

    private fun addInnerPanelForDestinationPackageField(
        panel: JPanel,
        gbConstraints: GridBagConstraints,
        targetField: ReferenceEditorComboWithBrowseButton
    )
    {
        val innerPanel = createInnerPanelForDestinationPackageField(targetField)
        panel.add(innerPanel, gbConstraints)
    }

    private fun createInnerPanelForDestinationPackageField(targetField: ReferenceEditorComboWithBrowseButton): JPanel
    {
        val innerPanel = JPanel(BorderLayout())
        innerPanel.add(targetField, BorderLayout.CENTER)
        return innerPanel
    }
}
