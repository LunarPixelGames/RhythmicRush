package io.github.msameer0.rhythmicrush.game.engine;

import com.badlogic.gdx.utils.Array;

/**
 * Simple generic object pool.
 * Call {@link #obtain()} to get an instance (creates one if the pool is empty),
 * and {@link #free(Object)} to return it for reuse.
 * <p>
 * The pool never shrinks — freed objects are kept indefinitely so subsequent
 * level resets allocate nothing and produce no GC pressure.
 */
public abstract class ObjectPool<T> {

    private final Array<T> free = new Array<>();

    /**
     * Create a new instance. Called only when the pool is empty.
     */
    protected abstract T create();

    /**
     * Reset the object's state before it is handed out again.
     */
    protected abstract void reset(T obj);

    /**
     * Get an instance from the pool, or create one if empty.
     */
    public T obtain() {
        if (free.size == 0) return create();
        return free.pop();
    }

    /**
     * Return an object to the pool.
     */
    public void free(T obj) {
        free.add(obj);
    }

    /**
     * Return all objects in a list to the pool and clear the list.
     */
    public void freeAll(Array<T> list) {
        free.addAll(list);
        list.clear();
    }

    public int getFreeCount() {
        return free.size;
    }
}
