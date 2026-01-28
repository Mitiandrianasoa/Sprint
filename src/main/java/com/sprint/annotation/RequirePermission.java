package com.sprint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour exiger une permission spécifique
 * Sprint 11 bis - Gestion avancée des rôles
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequirePermission {
    
    /**
     * Permission requise
     */
    String value();
    
    /**
     * Permissions alternatives (une seule suffit)
     */
    String[] alternatives() default {};
    
    /**
     * Message d'erreur personnalisé
     */
    String message() default "Permission requise: %s";
    
    /**
     * URL de redirection
     */
    String redirect() default "/access-denied";
}
