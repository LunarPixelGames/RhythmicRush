package io.github.msameer0.rhythmicrush.game.registries

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Registry(
    val id: String
)
