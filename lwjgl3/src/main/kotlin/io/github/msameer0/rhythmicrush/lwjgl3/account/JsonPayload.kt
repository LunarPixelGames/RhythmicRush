package io.github.msameer0.rhythmicrush.lwjgl3.account

import java.net.URLEncoder

internal object JsonPayload {
    fun encode(values: Map<String, Any?>): String = encodeValue(values)

    fun form(values: Map<String, String>): String = values.entries.joinToString("&") {
        "${url(it.key)}=${url(it.value)}"
    }

    private fun url(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encodeValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> quote(value)
        is Boolean, is Byte, is Short, is Int, is Long, is Float, is Double -> value.toString()
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") {
            quote(it.key.toString()) + ":" + encodeValue(it.value)
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { encodeValue(it) }
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { encodeValue(it) }
        else -> quote(value.toString())
    }

    private fun quote(value: String): String = buildString(value.length + 2) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
        append('"')
    }
}
