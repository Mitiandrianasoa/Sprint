package com.sprint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour mapper les requêtes HTTP GET aux méthodes de contrôleur.
 * C'est une version spécialisée de l'annotation @Test pour les requêtes GET.
 * Exemple d'utilisation :
 * - @Get("/users")
 * - @Get("/users/{id}")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Get {
    /**
     * Le chemin de l'URL à mapper pour les requêtes GET.
     * Peut contenir des paramètres entre accolades, ex: "/users/{id}"
     */
    String value() default "";
    
    /**
     * Paramètres de requête requis
     */
    String[] params() default {};
    
    /**
     * En-têtes requis
     */
    String[] headers() default {};
}