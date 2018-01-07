package com.martin.intellij.plugin.mockbuilder.extension

fun String.lastCharIfDigit(): Int?
{
    return lastOrNull()?.takeIf { it.isDigit() }?.toString()?.toInt()
}
