package com.sprint.security;

import com.sprint.model.UserSession;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestionnaire de rôles et permissions
 * Sprint 11 bis - Gestion avancée des rôles avec autorisations
 */
public class RoleManager {
    
    // Cache des permissions pour optimisation
    private static final Map<String, Set<String>> permissionCache = new HashMap<>();
    
    /**
     * Vérifie si un utilisateur a une permission spécifique
     */
    public static boolean hasPermission(UserSession user, String permission) {
        if (user == null || !user.isAuthenticated()) {
            return Role.ANONYME.hasPermission(permission);
        }
        
        return user.getRoles().stream()
                .anyMatch(roleName -> {
                    Role role = Role.fromString(roleName);
                    return role.hasPermission(permission);
                });
    }
    
    /**
     * Vérifie si un utilisateur a un niveau de rôle suffisant
     */
    public static boolean hasMinimumLevel(UserSession user, Role minimumRole) {
        if (user == null || !user.isAuthenticated()) {
            return Role.ANONYME.hasLevelOrHigher(minimumRole);
        }
        
        return user.getRoles().stream()
                .anyMatch(roleName -> {
                    Role userRole = Role.fromString(roleName);
                    return userRole.hasLevelOrHigher(minimumRole);
                });
    }
    
    /**
     * Vérifie si un utilisateur peut accéder à une ressource nécessitant un rôle spécifique
     */
    public static boolean canAccess(UserSession user, String requiredRole) {
        if (!Role.isValidRole(requiredRole)) {
            return false;
        }
        
        Role targetRole = Role.fromString(requiredRole);
        return hasMinimumLevel(user, targetRole);
    }
    
    /**
     * Récupère le rôle le plus élevé d'un utilisateur
     */
    public static Role getHighestRole(UserSession user) {
        if (user == null || !user.isAuthenticated()) {
            return Role.ANONYME;
        }
        
        return user.getRoles().stream()
                .map(Role::fromString)
                .max(Comparator.comparingInt(Role::getLevel))
                .orElse(Role.ANONYME);
    }
    
    /**
     * Récupère toutes les permissions d'un utilisateur
     */
    public static Set<String> getAllPermissions(UserSession user) {
        if (user == null || !user.isAuthenticated()) {
            return new HashSet<>(Role.ANONYME.getPermissions());
        }
        
        String cacheKey = user.getRoles().toString();
        if (permissionCache.containsKey(cacheKey)) {
            return permissionCache.get(cacheKey);
        }
        
        Set<String> permissions = user.getRoles().stream()
                .map(Role::fromString)
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
        
        permissionCache.put(cacheKey, permissions);
        return permissions;
    }
    
    /**
     * Récupère les variables spécifiques pour un utilisateur
     */
    public static Map<String, Object> getRoleSpecificVariables(UserSession user) {
        Map<String, Object> variables = new HashMap<>();
        Role highestRole = getHighestRole(user);
        
        // Variables de base pour tous les rôles
        variables.put("currentRole", highestRole.name());
        variables.put("roleLevel", highestRole.getLevel());
        variables.put("roleDisplayName", highestRole.getDisplayName());
        variables.put("isAuthenticated", user != null && user.isAuthenticated());
        
        // Variables spécifiques au rôle
        String[] specificVars = {"maxSessionTime", "allowedPages", "canComment", 
                               "maxFileSize", "canDeleteComments", "canBanUsers", 
                               "canManageSystem", "accessLevel"};
        
        for (String varName : specificVars) {
            Object value = highestRole.getRoleSpecificVariable(varName);
            if (value != null) {
                variables.put(varName, value);
            }
        }
        
        // Variables additionnelles selon les rôles
        if (user != null && user.isAuthenticated()) {
            variables.put("userId", user.getUserId());
            variables.put("username", user.getUsername());
            variables.put("loginTime", user.getLoginTime());
            variables.put("lastActivity", user.getLastActivity());
            variables.put("sessionAge", System.currentTimeMillis() - user.getLoginTime());
            
            // Variables spécifiques multi-rôles
            if (user.hasRole("ADMIN") && user.hasRole("MODERATOR")) {
                variables.put("isHybridAdminMod", true);
            }
        }
        
        return variables;
    }
    
    /**
     * Vérifie si l'accès est autorisé pour une ressource
     */
    public static boolean isAccessAuthorized(UserSession user, String resource, String action) {
        String permission = resource.toUpperCase() + "_" + action.toUpperCase();
        return hasPermission(user, permission);
    }
    
    /**
     * Filtre les pages accessibles pour un utilisateur
     */
    public static List<String> getAccessiblePages(UserSession user) {
        Role highestRole = getHighestRole(user);
        Object pagesObj = highestRole.getRoleSpecificVariable("allowedPages");
        
        if (pagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> pages = (List<String>) pagesObj;
            return pages;
        }
        
        return Arrays.asList("/", "/login"); // Pages par défaut
    }
    
    /**
     * Vérifie si la session est valide selon le rôle
     */
    public static boolean isSessionValid(UserSession user) {
        if (user == null || !user.isAuthenticated()) {
            return true; // Session anonyme toujours valide
        }
        
        Role highestRole = getHighestRole(user);
        Object maxTimeObj = highestRole.getRoleSpecificVariable("maxSessionTime");
        
        if (maxTimeObj instanceof Long) {
            long maxSessionTime = (Long) maxTimeObj;
            return !user.isExpired(maxSessionTime);
        }
        
        return !user.isExpired(); // Timeout par défaut
    }
    
    /**
     * Met à jour les variables de rôle dans la session
     */
    public static void updateRoleVariablesInSession(UserSession user, Map<String, Object> session) {
        Map<String, Object> roleVars = getRoleSpecificVariables(user);
        session.putAll(roleVars);
    }
    
    /**
     * Crée un utilisateur avec un rôle spécifique
     */
    public static UserSession createUserWithRole(String userId, String username, String email, Role role) {
        UserSession user = new UserSession(userId, username, email);
        user.authenticate(Arrays.asList(role.name()));
        return user;
    }
    
    /**
     * Promeut un utilisateur vers un rôle supérieur
     */
    public static boolean promoteUser(UserSession user, Role newRole) {
        if (user == null || !user.isAuthenticated()) {
            return false;
        }
        
        Role currentHighest = getHighestRole(user);
        if (newRole.getLevel() > currentHighest.getLevel()) {
            user.addRole(newRole.name());
            return true;
        }
        
        return false;
    }
    
    /**
     * Rétrograde un utilisateur
     */
    public static boolean demoteUser(UserSession user, Role targetRole) {
        if (user == null || !user.isAuthenticated()) {
            return false;
        }
        
        // Retirer tous les rôles supérieurs au rôle cible
        List<String> rolesToRemove = user.getRoles().stream()
                .filter(roleName -> {
                    Role role = Role.fromString(roleName);
                    return role.getLevel() > targetRole.getLevel();
                })
                .collect(Collectors.toList());
        
        rolesToRemove.forEach(user::removeRole);
        
        // S'assurer que l'utilisateur a au moins le rôle cible
        if (!user.hasRole(targetRole.name())) {
            user.addRole(targetRole.name());
        }
        
        return true;
    }
    
    /**
     * Vide le cache de permissions
     */
    public static void clearPermissionCache() {
        permissionCache.clear();
    }
}
