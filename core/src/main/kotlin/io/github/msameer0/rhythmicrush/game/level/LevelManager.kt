package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array

/**
 * Responsible for scanning, loading, and providing access to all game levels.
 */
class LevelManager {
    private val levels = Array<LevelData?>()

    constructor() {
        Gdx.app.log("LevelManager", "Scanning and pre-loading all levels...")
        loadAll()
        Gdx.app.log("LevelManager", "Pre-loaded " + levels.size + " levels.")
    }

    private fun loadAll() {
        levels.clear()
        var index = 0
        while (true) {
            val fh = Gdx.files.internal("levels/$index.json")
            if (!fh.exists()) break
            try {
                val data = LevelSerializer.load(fh)
                if (data != null) {
                    levels.add(data)
                }
                index++
            } catch (e: Exception) {
                Gdx.app.error("LevelManager", "Failed to load level " + index + ": " + e.message)
                break
            }
        }
    }

    fun getLevels(): Array<LevelData?> {
        return levels
    }

    fun getLevel(index: Int): LevelData? {
        if (index >= 0 && index < levels.size) {
            return levels.get(index)
        }
        return null
    }

    fun getLevelCount(): Int {
        return levels.size
    }
}
