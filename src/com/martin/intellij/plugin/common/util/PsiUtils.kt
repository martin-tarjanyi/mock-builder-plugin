package com.martin.intellij.plugin.common.util

import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile

class PsiUtils
{
    companion object
    {
        fun createDirectoryByPackageName(subjectFile: PsiJavaFile, project: Project, packageName: String): PsiDirectory
        {
            val module = ModuleUtil.findModuleForFile(subjectFile.virtualFile, project) ?: throw IllegalStateException(
                    "Subject class is not related to module.")

            val baseDir = PackageUtil.findPossiblePackageDirectoryInModule(module, packageName)

            return PackageUtil.findOrCreateDirectoryForPackage(module, packageName, baseDir,
                    true) ?: throw RuntimeException("Not able to find/create destination directory.")
        }
    }
}
