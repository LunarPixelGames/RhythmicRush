package io.github.msameer0.rhythmicrush.account

data class AccountConfiguration(
    val environment: String,
    val firebaseProjectId: String,
    val firebaseWebApiKey: String?,
    val backendBaseUrl: String,
    val apiVersion: Int = 1,
    val contentVersion: Int = 1,
    val firebaseAuthEmulatorHost: String? = null,
    val firebaseAuthEmulatorPort: Int? = null,
    val localWorkerHost: String? = null,
    val localWorkerPort: Int? = null
) {
    val isValid: Boolean
        get() = environment.isNotBlank() &&
            firebaseProjectId.isNotBlank() &&
            isSafeBackendUrl() &&
            apiVersion > 0 &&
            contentVersion > 0

    private fun isSafeBackendUrl(): Boolean {
        if (backendBaseUrl.startsWith("https://")) return true
        return environment != "production" &&
            (backendBaseUrl.startsWith("http://localhost:") ||
                backendBaseUrl.startsWith("http://127.0.0.1:"))
    }

    companion object {
        fun unavailable() = AccountConfiguration(
            environment = "unavailable",
            firebaseProjectId = "",
            firebaseWebApiKey = null,
            backendBaseUrl = ""
        )
    }
}

fun interface AccountConfigurationProvider {
    fun getConfiguration(): AccountConfiguration
}
