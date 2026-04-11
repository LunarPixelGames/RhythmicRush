package io.github.msameer0.rhythmicrush.game.engine

/**
 * Interface for objects that can be updated over time and respond to input events.
 */
interface Tickable {
    fun tick(delta: Float)
    fun onInput(held: Boolean): Boolean {
        return true
    }
}
