package com.sprint.security;

import com.sprint.annotation.RequirePermission;
import com.sprint.annotation.RequireRole;
import com.sprint.annotation.Secured;
import com.sprint.model.ModelView;
import com.sprint.model.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Intercepteur de sécurité pour vérifier les permissions
 * Sprint 11 bis - Gestion avancée des rôles
 */
public class SecurityInterceptor {
    
    /**
     * Vérifie les permissions avant l'exécution d'une méthode
     */
    public static Object checkSecurity(Method method, UserSession user, 
                                     Map<String, Object> session,
                                     HttpServletRequest req, 
                                     HttpServletResponse resp) throws IOException {
        
        // 1. Vérifier l'annotation @Secured
        if (method.isAnnotationPresent(Secured.class)) {
            Secured secured = method.getAnnotation(Secured.class);
            Object result = checkSecuredAnnotation(secured, user, session, req, resp);
            if (result != null) {
                return result;
            }
        }
        
        // 2. Vérifier l'annotation @RequireRole
        if (method.isAnnotationPresent(RequireRole.class)) {
            RequireRole requireRole = method.getAnnotation(RequireRole.class);
            Object result = checkRequireRoleAnnotation(requireRole, user, req, resp);
            if (result != null) {
                return result;
            }
        }
        
        // 3. Vérifier l'annotation @RequirePermission
        if (method.isAnnotationPresent(RequirePermission.class)) {
            RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
            Object result = checkRequirePermissionAnnotation(requirePermission, user, req, resp);
            if (result != null) {
                return result;
            }
        }
        
        // 4. Mettre à jour les variables de rôle dans la session
        RoleManager.updateRoleVariablesInSession(user, session);
        
        return null; // Pas de blocage, continuer l'exécution
    }
    
    /**
     * Vérifie l'annotation @Secured
     */
    private static Object checkSecuredAnnotation(Secured secured, UserSession user, 
                                               Map<String, Object> session,
                                               HttpServletRequest req, 
                                               HttpServletResponse resp) throws IOException {
        
        // Vérifier si l'authentification est requise
        if ("true".equals(secured.requireAuth()) && (user == null || !user.isAuthenticated())) {
            return createAccessDeniedResponse(secured.errorMessage(), secured.redirectOnError());
        }
        
        // Vérifier les rôles spécifiés
        if (secured.roles().length > 0) {
            boolean hasRequiredRole = false;
            for (Role requiredRole : secured.roles()) {
                if (RoleManager.canAccess(user, requiredRole.name())) {
                    hasRequiredRole = true;
                    break;
                }
            }
            if (!hasRequiredRole) {
                return createAccessDeniedResponse(secured.errorMessage(), secured.redirectOnError());
            }
        }
        
        // Vérifier le niveau minimum
        if (secured.minimumLevel() != Role.ANONYME) {
            if (!RoleManager.hasMinimumLevel(user, secured.minimumLevel())) {
                return createAccessDeniedResponse(secured.errorMessage(), secured.redirectOnError());
            }
        }
        
        // Vérifier les permissions spécifiques
        if (secured.permissions().length > 0) {
            boolean hasPermission = false;
            for (String permission : secured.permissions()) {
                if (RoleManager.hasPermission(user, permission)) {
                    hasPermission = true;
                    break;
                }
            }
            if (!hasPermission) {
                return createAccessDeniedResponse(secured.errorMessage(), secured.redirectOnError());
            }
        }
        
        // Vérifier la ressource et l'action
        if (!secured.resource().isEmpty() && !secured.action().isEmpty()) {
            if (!RoleManager.isAccessAuthorized(user, secured.resource(), secured.action())) {
                return createAccessDeniedResponse(secured.errorMessage(), secured.redirectOnError());
            }
        }
        
        return null;
    }
    
    /**
     * Vérifie l'annotation @RequireRole
     */
    private static Object checkRequireRoleAnnotation(RequireRole requireRole, UserSession user,
                                                   HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        Role requiredRole = requireRole.value();
        
        if (requireRole.allowHigher()) {
            if (!RoleManager.hasMinimumLevel(user, requiredRole)) {
                String message = String.format(requireRole.message(), requiredRole.getDisplayName());
                return createAccessDeniedResponse(message, requireRole.redirect());
            }
        } else {
            if (!RoleManager.canAccess(user, requiredRole.name())) {
                String message = String.format(requireRole.message(), requiredRole.getDisplayName());
                return createAccessDeniedResponse(message, requireRole.redirect());
            }
        }
        
        return null;
    }
    
    /**
     * Vérifie l'annotation @RequirePermission
     */
    private static Object checkRequirePermissionAnnotation(RequirePermission requirePermission, UserSession user,
                                                         HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        String requiredPermission = requirePermission.value();
        
        // Vérifier la permission principale
        if (RoleManager.hasPermission(user, requiredPermission)) {
            return null;
        }
        
        // Vérifier les permissions alternatives
        for (String alternative : requirePermission.alternatives()) {
            if (RoleManager.hasPermission(user, alternative)) {
                return null;
            }
        }
        
        // Aucune permission trouvée
        String message = String.format(requirePermission.message(), requiredPermission);
        return createAccessDeniedResponse(message, requirePermission.redirect());
    }
    
    /**
     * Crée une réponse d'accès refusé
     */
    private static Object createAccessDeniedResponse(String errorMessage, String redirectUrl) throws IOException {
        // Pour une API JSON
        if (isApiRequest()) {
            return com.sprint.model.JsonResponse.error(errorMessage, 403);
        }
        
        // Pour une redirection vers une page
        ModelView mv = new ModelView("access-denied");
        mv.addData("errorMessage", errorMessage);
        mv.addData("redirectUrl", redirectUrl);
        return mv;
    }
    
    /**
     * Vérifie si c'est une requête API (simple implémentation)
     */
    private static boolean isApiRequest() {
        // Logique à implémenter selon vos besoins
        // Par exemple, vérifier le header Accept: application/json
        return false;
    }
    
    /**
     * Vérifie si la session est valide selon le rôle
     */
    public static boolean validateSession(UserSession user) {
        return RoleManager.isSessionValid(user);
    }
    
    /**
     * Ajoute les informations de sécurité à la réponse
     */
    public static void addSecurityHeaders(HttpServletResponse resp) {
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
    }
    
    /**
     * Journalise une tentative d'accès non autorisée
     */
    public static void logUnauthorizedAccess(UserSession user, String resource, String action) {
        String username = (user != null) ? user.getUsername() : "ANONYMOUS";
        System.err.println("[SECURITY] Accès non autorisé - User: " + username + 
                          ", Resource: " + resource + ", Action: " + action + 
                          ", Time: " + new java.util.Date());
    }
}
