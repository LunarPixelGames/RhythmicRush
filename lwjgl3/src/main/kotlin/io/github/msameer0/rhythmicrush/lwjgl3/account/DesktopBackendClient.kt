package io.github.msameer0.rhythmicrush.lwjgl3.account

import com.badlogic.gdx.utils.JsonValue
import io.github.msameer0.rhythmicrush.account.AccountErrorCode
import io.github.msameer0.rhythmicrush.account.AccountOperationError

internal class DesktopBackendClient(private val baseUrl: String) {
    fun request(
        method: String,
        path: String,
        idToken: String,
        body: String? = null,
        idempotencyKey: String? = null
    ): JsonValue {
        val headers = linkedMapOf(
            "Authorization" to "Bearer $idToken",
            "X-Request-ID" to java.util.UUID.randomUUID().toString()
        )
        if (body != null) headers["Content-Type"] = "application/json; charset=utf-8"
        if (idempotencyKey != null) headers["X-Idempotency-Key"] = idempotencyKey

        try {
            val response = DesktopHttp.json(
                method,
                baseUrl.trimEnd('/') + path,
                body,
                headers
            )
            if (!response.getBoolean("ok", false)) {
                throw DesktopBackendException(parseError(response, 500))
            }
            return response.get("data") ?: JsonValue(JsonValue.ValueType.`object`)
        } catch (exception: DesktopHttpException) {
            throw DesktopBackendException(parseError(exception.response, exception.status))
        }
    }

    private fun parseError(response: JsonValue, status: Int): AccountOperationError {
        val error = response.get("error")
        val code = when (error?.getString("code", "")) {
            "AUTH_REQUIRED", "AUTH_INVALID", "AUTH_EXPIRED" -> AccountErrorCode.AUTH_INVALID
            "EMAIL_VERIFICATION_REQUIRED" -> AccountErrorCode.EMAIL_VERIFICATION_REQUIRED
            "CONFLICT" -> AccountErrorCode.CONFLICT
            "RATE_LIMITED" -> AccountErrorCode.RATE_LIMITED
            "BAD_REQUEST" -> AccountErrorCode.VALIDATION
            "UPDATE_REQUIRED" -> AccountErrorCode.UPDATE_REQUIRED
            "FEATURE_NOT_READY" -> AccountErrorCode.UNAVAILABLE
            else -> if (status >= 500) AccountErrorCode.SERVER else AccountErrorCode.UNKNOWN
        }
        return AccountOperationError(
            code = code,
            userMessage = error?.getString(
                "message",
                "The account service could not complete the request."
            ) ?: "The account service could not complete the request.",
            retryable = status == 429 || status >= 500,
            retryAfterSeconds = error?.getInt("retryAfterSeconds", 0)?.takeIf { it > 0 },
            diagnosticMessage = "HTTP $status"
        )
    }
}

internal class DesktopBackendException(val error: AccountOperationError) :
    Exception(error.userMessage)
