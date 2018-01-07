package com.martin.intellij.plugin.mockbuilder

fun <T> MutableSet<T>.addIfMissing(element: T) : Boolean
{
    if (contains(element))
    {
        return false
    }

    add(element)
    return true
}

fun String.lastCharIfDigit(): Int?
{
    return lastOrNull()?.takeIf { it.isDigit() }?.toString()?.toInt()
}
