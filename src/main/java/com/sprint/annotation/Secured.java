package com.sprint.annotation;

import com.sprint.security.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour sécuriser les méthodes de controller
 * Sprint 11 bis - Gestion avancée des rôles
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Secured {
    
    /**
     * Rôles autorisés pour accéder à cette méthode
     */
    Role[] roles() default {};
    
    /**
     * Permissions spécifiques requises
     */
    String[] permissions() default {};
    
    /**
     * Niveau minimum requis (si spécifié, override les rôles)
     */
    Role minimumLevel() default Role.ANONYME;
    
    /**
     * Ressource spécifique à protéger
     */
    String resource() default "";
    
    /**
     * Action requise sur la ressource
     */
    String action() default "";
    
    /**
     * Message d'erreur personnalisé en cas d'accès refusé
     */
    String errorMessage() default "Accès refusé: permissions insuffisantes";
    
    /**
     * URL de redirection en cas d'accès refusé
     */
    String redirectOnError() default "/access-denied";
    
    /**
     * Si true, vérifie également l'authentification
     */
    String requireAuth() default "true";
}
