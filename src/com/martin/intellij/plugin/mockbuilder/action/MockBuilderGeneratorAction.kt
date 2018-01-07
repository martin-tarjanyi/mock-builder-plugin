package com.martin.intellij.plugin.mockbuilder.action

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction

class MockBuilderGeneratorAction : BaseCodeInsightAction()
{
    override fun getHandler(): CodeInsightActionHandler
    {
        return MockBuilderGeneratorActionHandler()
    }
}
