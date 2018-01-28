package com.martin.intellij.plugin.common.util

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.util.RefactoringUtil
import com.martin.intellij.plugin.mockbuilder.action.WriteActionWrapper
import org.jetbrains.jps.model.java.JavaSourceRootType

class PsiUtils
{
    companion object
    {
        fun createDirectoryByPackageName(subjectFile: PsiJavaFile, project: Project, packageName: String): PsiDirectory
        {
            val psiManager = PsiManager.getInstance(project)
            val module = ModuleUtil.findModuleForFile(subjectFile.virtualFile, project) ?: throw IllegalStateException(
                "Subject class is not related to module."
            )

            val baseTestDir =
                ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE).firstOrNull()?.let {
                        psiManager.findDirectory(it)
                    } ?: throw RuntimeException("No test source is found. Create test sources to execute this action.")

            return WriteActionWrapper(project, subjectFile) {
                RefactoringUtil.createPackageDirectoryInSourceRoot(
                    PackageWrapper(psiManager, packageName), baseTestDir.virtualFile
                )
            }.perform()
        }
    }
}
