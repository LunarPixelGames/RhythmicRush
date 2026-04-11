package io.github.msameer0.rhythmicrush.game.registries

import com.badlogic.gdx.utils.OrderedMap

/**
 * A generic registry that map string IDs to class types and factory functions for object creation.
 */
class GameRegistry<T> {
    val factories: OrderedMap<String, () -> T?> = OrderedMap()
    val classes: OrderedMap<String, Class<out T>> = OrderedMap()

    fun register(cls: Class<out T>, factory: () -> T?) {
        val annotation = cls.getAnnotation(Registry::class.java)
        requireNotNull(annotation) { "Class " + cls.name + " is missing @Registry annotation." }
        val id = annotation.id
        factories.put(id, factory)
        classes.put(id, cls)
    }

    fun has(id: String): Boolean {
        return factories.containsKey(id)
    }

    fun getIds(): com.badlogic.gdx.utils.ObjectMap.Keys<String> {
        return factories.keys()
    }

    fun create(id: String): T? {
        val factory = factories[id]
        return factory?.invoke()
    }

    fun getClass(id: String): Class<out T>? {
        return classes[id]
    }
}
