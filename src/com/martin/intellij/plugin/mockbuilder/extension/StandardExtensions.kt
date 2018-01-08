package com.martin.intellij.plugin.mockbuilder.extension

fun String.lastCharIfDigit(): Int?
{
    return lastOrNull()?.takeIf { it.isDigit() }?.toString()?.toInt()
}

fun MutableMap<String, Int>.addOccurence(key: String) : Int
{
    if (!containsKey(key))
    {
        put(key, 1)
        return 1
    } else
    {
        val value = get(key)!!
        val incrementedValue = value.plus(1)
        put(key, incrementedValue)

        return incrementedValue
    }
}
