package com.martin.intellij.plugin.unittest.model

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class TestCaseResources(val actualField: PsiField?,
                        val parameterFields: List<PsiField>,
                        val givenMethods: List<PsiMethod>,
                        val whenMethod: PsiMethod,
                        val thenMethod: PsiMethod?,
                        val testCaseMethod: PsiMethod)
