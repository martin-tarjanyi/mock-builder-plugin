package com.martin.intellij.plugin.mockbuilder.action

import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class WriteActionWrapper<T>(project: Project, psiFile: PsiFile, private val action: () -> T)  : WriteCommandAction<T>(project, psiFile)
{
    fun perform() : T
    {
        return execute().resultObject
    }

    override fun run(resultContainer: Result<T>)
    {
        val result = action()

        resultContainer.setResult(result)
    }
}
