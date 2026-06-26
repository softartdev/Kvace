package com.softartdev.kvace.feature.chat.data

internal fun String.toAutomaticChatTitle(): String {
    val collapsed = firstNonBlankLine()
        .collapseWhitespace()
    return when {
        collapsed.length <= AUTOMATIC_TITLE_MAX_LENGTH -> collapsed
        else -> collapsed.take(AUTOMATIC_TITLE_MAX_LENGTH).trimEnd() + TITLE_ELLIPSIS
    }
}

private fun String.firstNonBlankLine(): String {
    var lineStart = 0
    for (index in indices) {
        val char = this[index]
        if (char == '\n' || char == '\r') {
            val line = substring(lineStart, index)
            if (line.isNotBlank()) return line
            lineStart = index + 1
        }
    }
    return substring(lineStart).takeIf { it.isNotBlank() }.orEmpty()
}

private fun String.collapseWhitespace(): String {
    val result = StringBuilder(length)
    var previousWasWhitespace = false
    for (char in trim()) {
        if (char.isWhitespace()) {
            if (!previousWasWhitespace) {
                result.append(' ')
                previousWasWhitespace = true
            }
        } else {
            result.append(char)
            previousWasWhitespace = false
        }
    }
    return result.toString()
}

private const val AUTOMATIC_TITLE_MAX_LENGTH = 48
private const val TITLE_ELLIPSIS = "..."
