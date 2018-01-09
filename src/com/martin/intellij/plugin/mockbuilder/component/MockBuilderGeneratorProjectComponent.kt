package com.martin.intellij.plugin.mockbuilder.component

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile

interface MockBuilderGeneratorProjectComponent
{
//    companion object
//    {
//        fun getInstance(project: Project): MockBuilderGeneratorProjectComponent
//        {
//            return ServiceManager.getService(project, MockBuilderGeneratorProjectComponent::class.java)
//        }
//    }

    fun execute(originalJavaFile: PsiJavaFile, psiDirectory: PsiDirectory?): PsiClass
}
