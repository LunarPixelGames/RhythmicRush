package io.github.msameer0.rhythmicrush.game.registries

/**
 * Annotation to mark a class as a registered game element.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Registry(
    /**
     * The unique identifier for this element (e.g., "spike", "ship_portal").
     */
    val id: String
)
