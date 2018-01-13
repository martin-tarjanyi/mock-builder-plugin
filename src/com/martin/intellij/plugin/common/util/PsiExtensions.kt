package com.martin.intellij.plugin.common.util

import com.intellij.psi.PsiElementFactory

fun PsiElementFactory.createStatementFromText(text: String) = createStatementFromText(text, null)
