package io.github.msameer0.rhythmicrush.lwjgl3.account

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

internal object DesktopHttp {
    private const val MAX_RESPONSE_BYTES = 1024 * 1024

    fun json(
        method: String,
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): JsonValue {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "RhythmicRush/${System.getProperty("rhythmicrush.version", "dev")} desktop"
            )
            headers.forEach(connection::setRequestProperty)

            if (body != null) {
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                if (!headers.containsKey("Content-Type")) {
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { it.write(bytes) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val buffer = ByteArray(MAX_RESPONSE_BYTES + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = input.read(buffer, total, buffer.size - total)
                    if (read < 0) break
                    total += read
                }
                if (total > MAX_RESPONSE_BYTES) throw IllegalStateException("Response too large.")
                buffer.copyOf(total)
            } ?: ByteArray(0)
            val text = String(bytes, StandardCharsets.UTF_8)
            val parsed = if (text.isBlank()) JsonValue(JsonValue.ValueType.`object`)
            else JsonReader().parse(text)
            if (status !in 200..299) throw DesktopHttpException(status, parsed)
            return parsed
        } finally {
            connection.disconnect()
        }
    }
}

internal class DesktopHttpException(
    val status: Int,
    val response: JsonValue
) : Exception("HTTP $status")
