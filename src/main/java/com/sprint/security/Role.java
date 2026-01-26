package com.sprint.security;

import java.util.Arrays;
import java.util.List;

/**
 * Énumération des rôles avec niveaux d'accès et permissions
 * Sprint 11 bis - Gestion avancée des rôles
 */
public enum Role {
    
    ANONYME(0, "Anonyme", "Utilisateur non identifié", 
            Arrays.asList("VIEW_PUBLIC", "ACCESS_LOGIN")),
    
    USER(1, "Utilisateur", "Utilisateur authentifié basique", 
            Arrays.asList("VIEW_PUBLIC", "ACCESS_LOGIN", "VIEW_PROTECTED", "EDIT_PROFILE")),
    
    MODERATOR(2, "Modérateur", "Peut modérer le contenu", 
            Arrays.asList("VIEW_PUBLIC", "ACCESS_LOGIN", "VIEW_PROTECTED", "EDIT_PROFILE", 
                        "MODERATE_CONTENT", "VIEW_USER_LIST")),
    
    ADMIN(3, "Administrateur", "Accès complet au système", 
            Arrays.asList("VIEW_PUBLIC", "ACCESS_LOGIN", "VIEW_PROTECTED", "EDIT_PROFILE", 
                        "MODERATE_CONTENT", "VIEW_USER_LIST", "MANAGE_USERS", 
                        "SYSTEM_CONFIG", "VIEW_ADMIN_PANEL"));
    
    private final int level;
    private final String displayName;
    private final String description;
    private final List<String> permissions;
    
    Role(int level, String displayName, String description, List<String> permissions) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
        this.permissions = permissions;
    }
    
    /**
     * Vérifie si ce rôle a une permission spécifique
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Vérifie si ce rôle a un niveau supérieur ou égal à un autre rôle
     */
    public boolean hasLevelOrHigher(Role other) {
        return this.level >= other.level;
    }
    
    /**
     * Vérifie si ce rôle peut accéder aux ressources d'un autre rôle
     */
    public boolean canAccess(Role targetRole) {
        return this.level >= targetRole.level;
    }
    
    /**
     * Récupère un rôle par son nom
     */
    public static Role fromString(String roleName) {
        try {
            return Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ANONYME; // Rôle par défaut
        }
    }
    
    /**
     * Récupère un rôle par son niveau
     */
    public static Role fromLevel(int level) {
        for (Role role : values()) {
            if (role.level == level) {
                return role;
            }
        }
        return ANONYME;
    }
    
    /**
     * Vérifie si une chaîne correspond à un rôle valide
     */
    public static boolean isValidRole(String roleName) {
        try {
            Role.valueOf(roleName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    // Getters
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * Variables spécifiques par rôle
     */
    public Object getRoleSpecificVariable(String variableName) {
        switch (this) {
            case ANONYME:
                return getAnonymeVariable(variableName);
            case USER:
                return getUserVariable(variableName);
            case MODERATOR:
                return getModeratorVariable(variableName);
            case ADMIN:
                return getAdminVariable(variableName);
            default:
                return null;
        }
    }
    
    private Object getAnonymeVariable(String variableName) {
        switch (variableName) {
            case "maxSessionTime": return 30 * 60 * 1000L; // 30 minutes
            case "allowedPages": return Arrays.asList("/", "/login", "/register");
            case "canComment": return false;
            default: return null;
        }
    }
    
    private Object getUserVariable(String variableName) {
        switch (variableName) {
            case "maxSessionTime": return 2 * 60 * 60 * 1000L; // 2 heures
            case "allowedPages": return Arrays.asList("/", "/profile", "/protected");
            case "canComment": return true;
            case "maxFileSize": return 5 * 1024 * 1024L; // 5MB
            default: return null;
        }
    }
    
    private Object getModeratorVariable(String variableName) {
        switch (variableName) {
            case "maxSessionTime": return 8 * 60 * 60 * 1000L; // 8 heures
            case "allowedPages": return Arrays.asList("/", "/profile", "/protected", "/moderator", "/users");
            case "canComment": return true;
            case "maxFileSize": return 20 * 1024 * 1024L; // 20MB
            case "canDeleteComments": return true;
            case "canBanUsers": return true;
            default: return null;
        }
    }
    
    private Object getAdminVariable(String variableName) {
        switch (variableName) {
            case "maxSessionTime": return 24 * 60 * 60 * 1000L; // 24 heures
            case "allowedPages": return Arrays.asList("/*"); // Toutes les pages
            case "canComment": return true;
            case "maxFileSize": return 100 * 1024 * 1024L; // 100MB
            case "canDeleteComments": return true;
            case "canBanUsers": return true;
            case "canManageSystem": return true;
            case "accessLevel": return "FULL";
            default: return null;
        }
    }
    
    @Override
    public String toString() {
        return displayName + " (Level " + level + ")";
    }
}
