package io.github.msameer0.rhythmicrush.lwjgl3.account

import com.badlogic.gdx.utils.JsonValue
import io.github.msameer0.rhythmicrush.account.AccountErrorCode
import io.github.msameer0.rhythmicrush.account.AccountOperationError
import io.github.msameer0.rhythmicrush.account.AccountProfile
import io.github.msameer0.rhythmicrush.account.LinkedProvider

internal class DesktopFirebaseAuth(
    private val configuration: io.github.msameer0.rhythmicrush.account.AccountConfiguration
) {
    private val apiKey = requireNotNull(configuration.firebaseWebApiKey) {
        "Desktop Firebase Web API key is missing."
    }

    fun register(email: String, password: String): DesktopSession {
        val response = identity(
            "accounts:signUp",
            mapOf(
                "email" to email,
                "password" to password,
                "returnSecureToken" to true
            )
        )
        return sessionFromCamelCase(response)
    }

    fun login(email: String, password: String): DesktopSession {
        val response = identity(
            "accounts:signInWithPassword",
            mapOf(
                "email" to email,
                "password" to password,
                "returnSecureToken" to true
            )
        )
        return sessionFromCamelCase(response)
    }

    fun refresh(refreshToken: String): DesktopSession {
        val response = DesktopHttp.json(
            "POST",
            "$secureTokenBase/token?key=$apiKey",
            JsonPayload.form(
                mapOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken
                )
            ),
            mapOf("Content-Type" to "application/x-www-form-urlencoded")
        )
        return DesktopSession(
            idToken = response.requireString("id_token"),
            refreshToken = response.requireString("refresh_token"),
            uid = response.requireString("user_id"),
            expiresAt = System.currentTimeMillis() +
                response.getString("expires_in", "3600").toLong() * 1000L
        )
    }

    fun lookup(idToken: String, username: String?): AccountProfile {
        val response = identity("accounts:lookup", mapOf("idToken" to idToken))
        val user = response.get("users")?.firstOrNull()
            ?: throw DesktopAuthException(
                AccountOperationError(AccountErrorCode.AUTH_INVALID, "Account no longer exists.")
            )
        val providers = mutableSetOf<LinkedProvider>()
        val providerInfo = user.get("providerUserInfo")
        var entry = providerInfo?.child
        while (entry != null) {
            when (entry.getString("providerId", "")) {
                "password" -> providers.add(LinkedProvider.EMAIL_PASSWORD)
                "playgames.google.com" -> providers.add(LinkedProvider.PLAY_GAMES)
            }
            entry = entry.next
        }
        if (user.getString("passwordHash", "").isNotEmpty() || user.has("email")) {
            providers.add(LinkedProvider.EMAIL_PASSWORD)
        }
        return AccountProfile(
            uid = user.requireString("localId"),
            username = username,
            email = user.getString("email", null),
            emailVerified = user.getBoolean("emailVerified", false),
            providers = providers
        )
    }

    fun sendVerification(idToken: String) {
        identity(
            "accounts:sendOobCode",
            mapOf("requestType" to "VERIFY_EMAIL", "idToken" to idToken)
        )
    }

    fun sendPasswordReset(email: String) {
        identity(
            "accounts:sendOobCode",
            mapOf("requestType" to "PASSWORD_RESET", "email" to email)
        )
    }

    fun linkEmail(idToken: String, email: String, password: String): DesktopSession {
        val response = identity(
            "accounts:update",
            mapOf(
                "idToken" to idToken,
                "email" to email,
                "password" to password,
                "returnSecureToken" to true
            )
        )
        return sessionFromCamelCase(response)
    }

    fun delete(idToken: String) {
        identity("accounts:delete", mapOf("idToken" to idToken))
    }

    private fun identity(endpoint: String, body: Map<String, Any?>): JsonValue {
        try {
            return DesktopHttp.json(
                "POST",
                "$identityBase/$endpoint?key=$apiKey",
                JsonPayload.encode(body)
            )
        } catch (exception: DesktopHttpException) {
            throw DesktopAuthException(parseError(exception))
        }
    }

    private fun sessionFromCamelCase(response: JsonValue): DesktopSession =
        DesktopSession(
            idToken = response.requireString("idToken"),
            refreshToken = response.requireString("refreshToken"),
            uid = response.requireString("localId"),
            expiresAt = System.currentTimeMillis() +
                response.getString("expiresIn", "3600").toLong() * 1000L
        )

    private fun parseError(exception: DesktopHttpException): AccountOperationError {
        val message = exception.response.get("error")?.getString("message", "")
            ?.substringBefore(" : ")
            .orEmpty()
        val code = when (message) {
            "EMAIL_EXISTS" -> AccountErrorCode.CONFLICT
            "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS",
            "INVALID_ID_TOKEN", "INVALID_REFRESH_TOKEN", "TOKEN_EXPIRED",
            "USER_NOT_FOUND", "USER_DISABLED" -> AccountErrorCode.AUTH_INVALID
            "WEAK_PASSWORD", "INVALID_EMAIL", "MISSING_PASSWORD" -> AccountErrorCode.VALIDATION
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> AccountErrorCode.RATE_LIMITED
            "OPERATION_NOT_ALLOWED" -> AccountErrorCode.UNAVAILABLE
            else -> if (exception.status >= 500) AccountErrorCode.SERVER else AccountErrorCode.UNKNOWN
        }
        val userMessage = when (message) {
            "EMAIL_EXISTS" -> "That email already has a Rhythmic Rush account. Sign in with email instead, then link another login method from the account screen."
            "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" ->
                "The email or password is incorrect."
            "WEAK_PASSWORD" -> "Choose a stronger password."
            "INVALID_EMAIL" -> "Enter a valid email address."
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Please wait before trying again."
            "USER_DISABLED" -> "This account has been disabled."
            "OPERATION_NOT_ALLOWED" -> "Email/password login is not enabled."
            else -> message.takeIf { it.isNotBlank() }
                ?: "Firebase authentication could not complete the request."
        }
        return AccountOperationError(
            code = code,
            userMessage = userMessage,
            retryable = code == AccountErrorCode.RATE_LIMITED || code == AccountErrorCode.SERVER,
            diagnosticMessage = message.ifBlank { "HTTP ${exception.status}" }
        )
    }

    private val identityBase: String
        get() {
            val host = configuration.firebaseAuthEmulatorHost
            val port = configuration.firebaseAuthEmulatorPort
            return if (host != null && port != null) {
                "http://$host:$port/identitytoolkit.googleapis.com/v1"
            } else {
                "https://identitytoolkit.googleapis.com/v1"
            }
        }

    private val secureTokenBase: String
        get() {
            val host = configuration.firebaseAuthEmulatorHost
            val port = configuration.firebaseAuthEmulatorPort
            return if (host != null && port != null) {
                "http://$host:$port/securetoken.googleapis.com/v1"
            } else {
                "https://securetoken.googleapis.com/v1"
            }
        }
}

internal data class DesktopSession(
    val idToken: String,
    val refreshToken: String,
    val uid: String,
    val expiresAt: Long
) {
    fun needsRefresh(now: Long = System.currentTimeMillis()): Boolean =
        now >= expiresAt - 60_000L
}

internal class DesktopAuthException(val error: AccountOperationError) :
    Exception(error.userMessage)

private fun JsonValue.requireString(name: String): String =
    getString(name, null)
        ?: throw IllegalStateException("Firebase response is missing $name.")

private fun JsonValue.firstOrNull(): JsonValue? = child
