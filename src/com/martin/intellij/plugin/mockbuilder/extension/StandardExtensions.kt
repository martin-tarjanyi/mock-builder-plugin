package com.martin.intellij.plugin.mockbuilder.extension


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

fun String.findIndefiniteArticle() : String
{
    val firstCharacter = toCharArray()[0]

    return when (firstCharacter.toLowerCase())
    {
        'a', 'e', 'i', 'o', 'u' -> "an"
        else -> "a"
    }
}
