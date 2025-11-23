package com.sprint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour mapper les requêtes HTTP aux méthodes de contrôleur.
 * Exemple d'utilisation :
 * - @Test("/users")
 * - @Test("/users/{id}")
 * - @Test("/users/{id}/posts/{postId}")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
    /**
     * Le chemin de l'URL à mapper.
     * Peut contenir des paramètres entre accolades, ex: "/users/{id}"
     */
    String value() default "";
    
    /**
     * Méthodes HTTP supportées (GET, POST, etc.)
     */
    String[] method() default {};
    
    /**
     * Paramètres de requête requis
     */
    String[] params() default {};
    
    /**
     * En-têtes requis
     */
    String[] headers() default {};
}