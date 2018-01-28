package com.martin.intellij.plugin.unittest.component

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.martin.intellij.plugin.unittest.model.TestCaseResources

interface TestCaseResourceMapperComponent
{
    fun map(methodToTest: PsiMethod,
            givenStepsForMocks: List<PsiMethod>,
            mockedFields: List<PsiField>,
            subjectClass: PsiClass) : TestCaseResources
}
