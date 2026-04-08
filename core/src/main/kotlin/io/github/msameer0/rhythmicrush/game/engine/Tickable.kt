package io.github.msameer0.rhythmicrush.game.engine

interface Tickable {
    fun tick(delta: Float)
    fun onInput(held: Boolean) : Boolean {
        return true
    }
}
