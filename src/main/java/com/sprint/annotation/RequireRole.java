package com.sprint.annotation;

import com.sprint.security.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour exiger un rôle spécifique
 * Sprint 11 bis - Gestion avancée des rôles
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    
    /**
     * Rôle requis
     */
    Role value();
    
    /**
     * Si true, accepte aussi les rôles de niveau supérieur
     */
    boolean allowHigher() default true;
    
    /**
     * Message d'erreur personnalisé
     */
    String message() default "Rôle requis: %s";
    
    /**
     * URL de redirection
     */
    String redirect() default "/access-denied";
}
