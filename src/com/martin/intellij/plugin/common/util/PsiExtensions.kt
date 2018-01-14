package com.martin.intellij.plugin.common.util

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.InheritanceUtil

fun PsiElementFactory.createStatementFromText(text: String) = createStatementFromText(text, null)

fun PsiMethod.isPublic() = modifierList.hasModifierProperty(PsiModifier.PUBLIC)

fun PsiType.generateName(): String
{
    return when (this)
    {
        is PsiClassReferenceType -> {

            when
            {
                isCollectionType(this) -> nameTypeBasedOnGenericParameter(this)

                else -> this.className.decapitalize()
            }
        }

        is PsiPrimitiveType -> this.let { mapPrimitive(it) }
        is PsiArrayType -> this.presentableText.decapitalize().removeSuffix("[]") + "s"
        else -> throw RuntimeException("Unexpected type: $this")
    }
}

fun PsiElementFactory.createPrivateMethod(name: String, returnType: PsiType): PsiMethod
{
    val createdMethod = createMethod(name, returnType)
    createdMethod.modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
    return createdMethod
}

private fun nameTypeBasedOnGenericParameter(returnType: PsiClassReferenceType): String {
    return ((returnType.parameters.getOrNull(0)
            as? PsiClassReferenceType)
            ?.let { "${it.name}s" }?.decapitalize()
            ?: returnType.className.decapitalize())
}

private fun isCollectionType(returnType: PsiType?) =
        InheritanceUtil.isInheritor(returnType, "java.util.Collection")

private fun mapPrimitive(primitiveType: PsiPrimitiveType): String = when (primitiveType.name)
{
    "int", "long", "short" -> "number"
    else -> "primitive"
}
