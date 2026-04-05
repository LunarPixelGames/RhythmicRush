package io.github.msameer0.rhythmicrush.game.registries

import java.util.function.Supplier

class GameRegistry<T> {
    val factories: MutableMap<String?, Supplier<T?>?> =
        LinkedHashMap<String?, Supplier<T?>?>()
    val classes: MutableMap<String?, Class<out T?>?> =
        LinkedHashMap<String?, Class<out T?>?>()

    fun register(cls: Class<out T?>, factory: Supplier<T?>?) {
        val annotation = cls.getAnnotation(Registry::class.java)
        requireNotNull(annotation) { "Class " + cls.getName() + " is missing @Registry annotation." }
        val id = annotation.id
        factories[id] = factory
        classes[id] = cls
    }

    fun has(id: String?): Boolean {
        return factories.containsKey(id)
    }

    fun getIds(): MutableSet<String?> {
        return factories.keys
    }

    fun create(id: String?): T? {
        val factory = factories[id]
        return factory?.get()
    }

    fun getClass(id: String?): Class<out T?>? {
        return classes[id]
    }
}
