package com.martin.intellij.plugin.mockbuilder

fun String.lastCharIfDigit(): Int?
{
    return lastOrNull()?.takeIf { it.isDigit() }?.toString()?.toInt()
}
