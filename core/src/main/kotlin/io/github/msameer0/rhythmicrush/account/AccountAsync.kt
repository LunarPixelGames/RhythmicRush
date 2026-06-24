package io.github.msameer0.rhythmicrush.account

import com.badlogic.gdx.Gdx
import java.util.concurrent.atomic.AtomicBoolean

interface AccountOperation {
    val isCancelled: Boolean
    fun cancel()
}

interface AccountCallback<T> {
    fun onSuccess(value: T)
    fun onFailure(error: AccountOperationError)
}

class SimpleAccountOperation : AccountOperation {
    private val cancelled = AtomicBoolean(false)

    override val isCancelled: Boolean
        get() = cancelled.get()

    override fun cancel() {
        cancelled.set(true)
    }
}

fun <T> deliverSuccess(
    operation: AccountOperation,
    callback: AccountCallback<T>,
    value: T
) {
    Gdx.app.postRunnable {
        if (!operation.isCancelled) callback.onSuccess(value)
    }
}

fun <T> deliverFailure(
    operation: AccountOperation,
    callback: AccountCallback<T>,
    error: AccountOperationError
) {
    Gdx.app.postRunnable {
        if (!operation.isCancelled) callback.onFailure(error)
    }
}
