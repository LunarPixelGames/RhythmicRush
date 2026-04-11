package io.github.msameer0.rhythmicrush.game.registries

/**
 * Annotation used to define a unique string ID for a class to be used in a GameRegistry.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Registry(
    val id: String
)
