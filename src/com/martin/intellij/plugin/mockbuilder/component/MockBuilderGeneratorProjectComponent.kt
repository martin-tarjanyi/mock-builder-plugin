package com.martin.intellij.plugin.mockbuilder.component

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory

interface MockBuilderGeneratorProjectComponent
{
    fun execute(subjectClass: PsiClass, psiDirectory: PsiDirectory): PsiClass
}
