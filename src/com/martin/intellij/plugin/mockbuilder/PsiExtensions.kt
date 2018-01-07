package com.martin.intellij.plugin.mockbuilder

import com.intellij.psi.PsiElementFactory

fun PsiElementFactory.createStatementFromText(text: String)= createStatementFromText(text, null)
