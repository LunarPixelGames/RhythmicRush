package io.github.msameer0.rhythmicrush.game.registries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A generic registry to map string IDs to object factories.
 * @param <T> The base type of objects in this registry.
 */
public class GameRegistry<T> {
    private final Map<String, Supplier<T>> factories = new LinkedHashMap<>();
    private final Map<String, Class<? extends T>> classes = new LinkedHashMap<>();

    /**
     * Registers a class and its factory.
     * @param cls The class to register.
     * @param factory The factory to create instances of the class.
     */
    public void register(Class<? extends T> cls, Supplier<T> factory) {
        Registry annotation = cls.getAnnotation(Registry.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is missing @Registry annotation.");
        }
        String id = annotation.id();
        factories.put(id, factory);
        classes.put(id, cls);
    }

    /**
     * Checks if an ID is registered.
     * @param id The ID to check.
     * @return true if registered, false otherwise.
     */
    public boolean has(String id) {
        return factories.containsKey(id);
    }

    /** Returns all registered IDs in insertion order. */
    public java.util.Set<String> getIds() {
        return factories.keySet();
    }

    /**
     * Creates a new instance of the object registered with the given ID.
     * @param id The ID of the object to create.
     * @return A new instance, or null if not registered.
     */
    public T create(String id) {
        Supplier<T> factory = factories.get(id);
        return factory != null ? factory.get() : null;
    }

    /**
     * Gets the class associated with an ID.
     * @param id The ID to look up.
     * @return The registered class, or null.
     */
    public Class<? extends T> getClass(String id) {
        return classes.get(id);
    }
}
