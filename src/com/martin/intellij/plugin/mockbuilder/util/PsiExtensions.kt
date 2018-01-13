package com.martin.intellij.plugin.mockbuilder.util

import com.intellij.psi.PsiElementFactory

fun PsiElementFactory.createStatementFromText(text: String)= createStatementFromText(text, null)
