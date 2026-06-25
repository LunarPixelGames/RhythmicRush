package io.github.msameer0.rhythmicrush.lwjgl3.account

import io.github.msameer0.rhythmicrush.account.AccountConfiguration
import io.github.msameer0.rhythmicrush.account.AccountConfigurationProvider
import java.io.File
import java.util.Properties

class DesktopAccountConfiguration : AccountConfigurationProvider {
    override fun getConfiguration(): AccountConfiguration {
        val properties = loadProperties()
        if (properties == null) {
            return AccountConfiguration(
                environment = "production",
                firebaseProjectId = "rhythmic-rush",
                firebaseWebApiKey = "AIzaSyAdkIaAWHybv8dO-i-NpY3Mnc3u1G0J_1M",
                backendBaseUrl = "https://rhythmic-rush-api.sameerthecoolguy2006.workers.dev",
                apiVersion = 1,
                contentVersion = 1
            )
        }
        val environment = properties.getProperty("environment", "development").trim()
        val localWorkerHost = properties.getProperty("localWorkerHost")?.trim()?.takeIf { it.isNotEmpty() }
        val localWorkerPort = properties.getProperty("localWorkerPort")?.toIntOrNull()
        val configuredBackend = properties.getProperty("backendBaseUrl", "").trim()
        val backendBaseUrl = if (localWorkerHost != null && localWorkerPort != null) {
            "http://$localWorkerHost:$localWorkerPort"
        } else {
            configuredBackend
        }

        return AccountConfiguration(
            environment = environment,
            firebaseProjectId = properties.getProperty("firebaseProjectId", "").trim(),
            firebaseWebApiKey = properties.getProperty("firebaseWebApiKey")
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            backendBaseUrl = backendBaseUrl,
            apiVersion = properties.getProperty("apiVersion", "1").toIntOrNull() ?: 1,
            contentVersion = properties.getProperty("contentVersion", "1").toIntOrNull() ?: 1,
            firebaseAuthEmulatorHost = properties.getProperty("firebaseAuthEmulatorHost")
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            firebaseAuthEmulatorPort =
                properties.getProperty("firebaseAuthEmulatorPort")?.toIntOrNull(),
            localWorkerHost = localWorkerHost,
            localWorkerPort = localWorkerPort
        )
    }

    private fun loadProperties(): Properties? {
        val explicit = System.getProperty("rhythmicrush.account.config")
            ?.let(::File)
        val candidates = listOfNotNull(
            explicit,
            File("account-prod.properties"),
            File("../lwjgl3/account-prod.properties"),
            File("lwjgl3/account-prod.properties"),
            File("account-dev.properties"),
            File("../lwjgl3/account-dev.properties"),
            File("lwjgl3/account-dev.properties")
        )
        val file = candidates.firstOrNull { it.isFile } ?: return null
        return Properties().apply {
            file.inputStream().use(::load)
        }
    }
}
