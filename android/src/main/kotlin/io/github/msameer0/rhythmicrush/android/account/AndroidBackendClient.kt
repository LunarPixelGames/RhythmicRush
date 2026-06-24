package io.github.msameer0.rhythmicrush.android.account

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import io.github.msameer0.rhythmicrush.account.AccountErrorCode
import io.github.msameer0.rhythmicrush.account.AccountOperation
import io.github.msameer0.rhythmicrush.account.AccountOperationError
import io.github.msameer0.rhythmicrush.account.SimpleAccountOperation
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService

internal class AndroidBackendClient(
    private val baseUrl: String,
    private val auth: FirebaseAuth,
    private val appCheck: FirebaseAppCheck,
    private val executor: ExecutorService
) {
    fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        idempotencyKey: String? = null,
        forceRefreshIdToken: Boolean = false,
        operation: SimpleAccountOperation = SimpleAccountOperation(),
        callback: (Result<JSONObject>) -> Unit
    ): AccountOperation {
        val user = auth.currentUser
        if (user == null) {
            callback(Result.failure(BackendException(
                AccountOperationError(
                    AccountErrorCode.AUTH_REQUIRED,
                    "Please sign in first."
                )
            )))
            return operation
        }

        user.getIdToken(forceRefreshIdToken)
            .addOnSuccessListener { tokenResult ->
                val idToken = tokenResult.token
                if (idToken.isNullOrBlank()) {
                    callback(Result.failure(BackendException(
                        AccountOperationError(
                            AccountErrorCode.AUTH_INVALID,
                            "Could not refresh your account session."
                        )
                    )))
                    return@addOnSuccessListener
                }

                appCheck.getAppCheckToken(false)
                    .addOnCompleteListener { appCheckTask ->
                        val appCheckToken =
                            if (appCheckTask.isSuccessful) appCheckTask.result.token else null
                        executor.execute {
                            if (operation.isCancelled) return@execute
                            callback(runCatching {
                                execute(
                                    method,
                                    path,
                                    body,
                                    idToken,
                                    appCheckToken,
                                    idempotencyKey
                                )
                            })
                        }
                    }
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(BackendException(mapThrowable(exception))))
            }

        return operation
    }

    private fun execute(
        method: String,
        path: String,
        body: JSONObject?,
        idToken: String,
        appCheckToken: String?,
        idempotencyKey: String?
    ): JSONObject {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $idToken")
            connection.setRequestProperty("X-Request-ID", java.util.UUID.randomUUID().toString())
            if (!appCheckToken.isNullOrBlank()) {
                connection.setRequestProperty("X-Firebase-AppCheck", appCheckToken)
            }
            if (!idempotencyKey.isNullOrBlank()) {
                connection.setRequestProperty("X-Idempotency-Key", idempotencyKey)
            }

            if (body != null) {
                val bytes = body.toString().toByteArray(StandardCharsets.UTF_8)
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(bytes) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299 || !response.optBoolean("ok", false)) {
                throw BackendException(parseBackendError(response, status))
            }
            return response.optJSONObject("data") ?: JSONObject()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseBackendError(response: JSONObject, status: Int): AccountOperationError {
        val error = response.optJSONObject("error")
        val code = when (error?.optString("code")) {
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
            userMessage = error?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: "The account service could not complete the request.",
            retryable = status == 429 || status >= 500,
            retryAfterSeconds = error?.optInt("retryAfterSeconds")
                ?.takeIf { it > 0 },
            diagnosticMessage = "HTTP $status"
        )
    }

    companion object {
        fun mapThrowable(throwable: Throwable): AccountOperationError {
            val name = throwable.javaClass.simpleName
            val network = name.contains("Network", ignoreCase = true) ||
                name.contains("Timeout", ignoreCase = true) ||
                throwable is java.io.IOException
            return AccountOperationError(
                code = if (network) AccountErrorCode.NETWORK else AccountErrorCode.UNKNOWN,
                userMessage = if (network) {
                    "Could not reach the account service."
                } else {
                    throwable.message ?: "The account operation failed."
                },
                retryable = network,
                diagnosticMessage = name
            )
        }
    }
}

internal class BackendException(val accountError: AccountOperationError) :
    Exception(accountError.userMessage)
