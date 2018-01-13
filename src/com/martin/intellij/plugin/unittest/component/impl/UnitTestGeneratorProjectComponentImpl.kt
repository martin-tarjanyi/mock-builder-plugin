package com.martin.intellij.plugin.unittest.component.impl

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.martin.intellij.plugin.mockbuilder.component.MockBuilderGeneratorProjectComponent
import com.martin.intellij.plugin.unittest.component.UnitTestGeneratorProjectComponent

class UnitTestGeneratorProjectComponentImpl(
        private val mockBuilderGeneratorProjectComponent: MockBuilderGeneratorProjectComponent) : UnitTestGeneratorProjectComponent
{
    override fun execute(subjectClass: PsiClass, psiDirectory: PsiDirectory): PsiClass
    {
        TODO("Implement")
    }
}
