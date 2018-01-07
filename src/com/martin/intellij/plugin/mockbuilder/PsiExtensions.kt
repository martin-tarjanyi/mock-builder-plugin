package com.martin.intellij.plugin.mockbuilder

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory

fun PsiElementFactory.createStatementFromText(text: String)= createStatementFromText(text, null)

fun getEditor(element: PsiElement): Editor?
{
    val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
    if (document != null)
    {
        val instance = EditorFactory.getInstance() ?: return null
        val editors = instance.getEditors(document)
        if (editors.isNotEmpty())
        {
            return editors[0]
        }
    }
    return null
}
