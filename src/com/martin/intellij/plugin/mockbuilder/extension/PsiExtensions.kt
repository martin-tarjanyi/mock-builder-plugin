package com.martin.intellij.plugin.mockbuilder.extension

import com.intellij.psi.PsiElementFactory

fun PsiElementFactory.createStatementFromText(text: String)= createStatementFromText(text, null)
