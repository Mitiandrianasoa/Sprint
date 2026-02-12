package com.sprint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Session {
    /**
     * Nom de la session (optionnel)
     * Si non spécifié, utilise "default"
     */
    String value() default "default";
    
    /**
     * Si true, crée une nouvelle session si elle n'existe pas
     */
    boolean create() default true;
}
