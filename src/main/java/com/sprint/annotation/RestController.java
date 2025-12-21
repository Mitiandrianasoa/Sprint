package com.sprint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour indiquer qu'un contrôleur est un contrôleur REST
 * qui retourne du JSON au lieu de vues.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestController {
    /**
     * Préfixe pour toutes les routes du contrôleur
     */
    String value() default "";
    
    /**
     * Type de contenu par défaut (peut être overridden par méthode)
     */
    String produces() default "application/json";
}