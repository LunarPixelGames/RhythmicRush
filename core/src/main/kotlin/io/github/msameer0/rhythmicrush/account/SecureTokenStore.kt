package io.github.msameer0.rhythmicrush.account

interface SecureTokenStore {
    fun load(key: String): String?
    fun save(key: String, value: String): Boolean
    fun delete(key: String)
    fun clear()
}

class InMemorySecureTokenStore : SecureTokenStore {
    private val values = mutableMapOf<String, String>()

    override fun load(key: String): String? = values[key]

    override fun save(key: String, value: String): Boolean {
        values[key] = value
        return true
    }

    override fun delete(key: String) {
        values.remove(key)
    }

    override fun clear() {
        values.clear()
    }
}
