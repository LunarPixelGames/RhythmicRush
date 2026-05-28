package io.github.msameer0.rhythmicrush

import com.badlogic.gdx.graphics.Color

/**
 * Centralized hub for all tunable game parameters.
 * Modify these values to adjust physics, camera behavior, and visual defaults globally.
 */
object GameConstants {

    object World {
        const val SCROLL_SPEED = 986.35f
        const val GROUND_Y = 304f
        const val DELTA_CAP = 0.1f
    }

    object Player {
        const val PLAYER_SIZE = 100f
        const val START_X = 100f

        object Cube {
            const val JUMP_VELOCITY = 1906.2f
            const val GRAVITY = -8450.2f
            const val SPIN_SPEED = 399.0f
            const val COYOTE_TIME = 0.083f
        }

        object Ship {
            const val MAX_UP_SPEED = 986.35f
            const val MAX_DOWN_SPEED = -986.35f
            const val ACCEL = 2432.2f
            const val DECEL = 2432.2f
            const val TILT_EXAGGERATION = 1.4f
            const val TILT_LERP = 12f
            const val MAX_TILT = 50f
        }
    }

    object Camera {
        const val X_OFFSET = 300f
        const val X_LERP = 5f
        const val Y_LERP = 15f
        const val SMOOTH_LERP = 6f
        const val CORRIDOR_HEIGHT = 1000f
        const val TRANSITION_SPEED = 4f
        const val PADDING_HEIGHT = 500f
    }

    object Rendering {
        val DEFAULT_BG_COLOR = Color.valueOf("0000ff")
        val DEFAULT_GROUND_COLOR = Color.valueOf("000080")
        const val BOUNDARY_LINE_WIDTH = 5f
    }

    object Editor {
        const val GRID_SIZE = 100f
        const val SIDEBAR_W = 260f
        const val TOPBAR_H = 48f
        const val ITEM_PAD = 6f
        const val ITEM_SIZE = 48f
        const val TAB_H = 34f
        const val CAM_SPEED = 400f
    }

    object Interactables {
        object Pads {
            const val PINK_VELOCITY = 2055f
            const val YELLOW_VELOCITY = 3050f
            const val RED_VELOCITY = 3900f
            const val GREEN_VELOCITY = 3050f
            const val BLUE_VELOCITY = 600f
            const val BLACK_VELOCITY = 2200f
        }

        object Orbs {
            const val PINK_VELOCITY = 1840f
            const val YELLOW_VELOCITY = 2430f
            const val RED_VELOCITY = 3180f
            const val GREEN_VELOCITY = 2430f
            const val BLACK_VELOCITY = 1800f
        }
    }
}
