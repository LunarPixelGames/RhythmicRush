package io.github.msameer0.rhythmicrush.update

/**
 * Interface for checking and managing application updates.
 */
interface UpdateManager {
    /**
     * Check for updates and start the update flow if available.
     * For Android: Uses in-app update API with IMMEDIATE flow for mandatory updates.
     * For Desktop: No-op.
     */
    fun checkForUpdate()

    /**
     * Immediately start a forced update (blocking).
     * Should be called when an update is mandatory and cannot be deferred.
     */
    fun forceUpdate()

    /**
     * Returns true if an update is currently in progress or pending installation.
     */
    fun isUpdatePending(): Boolean

    /**
     * Returns true if an update is available but not yet downloaded.
     */
    fun isUpdateAvailable(): Boolean

    /**
     * Lifecycle callback for onResume.
     */
    fun onResume() {}

    /**
     * Lifecycle callback for onStop.
     */
    fun onStop() {}

    /**
     * Lifecycle callback for onDestroy.
     */
    fun onDestroy() {}
}
