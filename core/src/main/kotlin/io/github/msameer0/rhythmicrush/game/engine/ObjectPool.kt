package io.github.msameer0.rhythmicrush.game.engine

import com.badlogic.gdx.utils.Array

/**
 * Generic object pooling system to minimize garbage collection by reusing objects.
 */
abstract class ObjectPool<T : Any> {
    val free = Array<T>()

    protected abstract fun create(): T
    protected abstract fun reset(obj: T)

    fun obtain(): T {
        if (free.size == 0) return create()
        val obj = free.pop()
        reset(obj)
        return obj
    }

    fun free(obj: T) {
        free.add(obj)
    }

    fun freeAll(list: Array<T>) {
        free.addAll(list)
        list.clear()
    }

    fun getFreeCount(): Int {
        return free.size
    }
}
