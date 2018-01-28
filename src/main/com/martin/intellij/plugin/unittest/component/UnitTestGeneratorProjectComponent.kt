package com.martin.intellij.plugin.unittest.component

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory

interface UnitTestGeneratorProjectComponent
{
    fun execute(
        subjectClass: PsiClass,
        unitTestPsiDirectory: PsiDirectory,
        mockBuilderPsiDirectory: PsiDirectory
    ): PsiClass
}
