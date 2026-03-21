package io.github.msameer0.rhythmicrush.game.registries;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a registered game element.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Registry {
    /**
     * The unique identifier for this element (e.g., "spike", "ship_portal").
     */
    String id();
}
