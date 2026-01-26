package com.sprint.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Classe représentant une session utilisateur avec gestion des rôles
 * Utilisée avec le système de session basé sur Map du framework Sprint
 */
public class UserSession {
    
    private String userId;
    private String username;
    private String email;
    private List<String> roles;
    private long loginTime;
    private long lastActivity;
    private boolean authenticated;
    private Map<String, Object> attributes;
    
    /**
     * Constructeur par défaut
     */
    public UserSession() {
        this.roles = new ArrayList<>();
        this.loginTime = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();
        this.authenticated = false;
    }
    
    /**
     * Constructeur avec identifiants
     */
    public UserSession(String userId, String username, String email) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
    
    /**
     * Crée une instance UserSession à partir d'une Map de session
     * @param sessionMap La Map contenant les données de session
     * @return UserSession si les données utilisateur sont trouvées, null sinon
     */
    public static UserSession fromSessionMap(Map<String, Object> sessionMap) {
        if (sessionMap == null || !sessionMap.containsKey("userSession")) {
            return null;
        }
        
        Object userSessionObj = sessionMap.get("userSession");
        if (userSessionObj instanceof UserSession) {
            UserSession userSession = (UserSession) userSessionObj;
            userSession.updateLastActivity();
            return userSession;
        }
        
        return null;
    }
    
    /**
     * Vérifie si l'utilisateur a un rôle spécifique
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Vérifie si l'utilisateur a au moins un des rôles spécifiés
     */
    public boolean hasAnyRole(String... rolesToCheck) {
        for (String role : rolesToCheck) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Vérifie si l'utilisateur a tous les rôles spécifiés
     */
    public boolean hasAllRoles(String... rolesToCheck) {
        for (String role : rolesToCheck) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Ajoute un rôle à l'utilisateur
     */
    public void addRole(String role) {
        if (!roles.contains(role)) {
            roles.add(role);
        }
    }
    
    /**
     * Retire un rôle à l'utilisateur
     */
    public void removeRole(String role) {
        roles.remove(role);
    }
    
    /**
     * Authentifie l'utilisateur avec ses rôles
     */
    public void authenticate(List<String> userRoles) {
        this.authenticated = true;
        this.roles.clear();
        if (userRoles != null) {
            this.roles.addAll(userRoles);
        }
        this.loginTime = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();
    }
    
    /**
     * Déconnecte l'utilisateur
     */
    public void logout() {
        this.authenticated = false;
        this.roles.clear();
    }
    
    /**
     * Met à jour le timestamp de dernière activité
     */
    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
    
    /**
     * Vérifie si la session est expirée (timeout par défaut: 30 minutes)
     */
    public boolean isExpired() {
        return isExpired(30 * 60 * 1000); // 30 minutes
    }
    
    /**
     * Vérifie si la session est expirée avec un timeout personnalisé
     */
    public boolean isExpired(long timeoutMillis) {
        return (System.currentTimeMillis() - lastActivity) > timeoutMillis;
    }
    
    /**
     * Sauvegarde cette instance dans la Map de session
     */
    public void saveToSessionMap(Map<String, Object> sessionMap) {
        if (sessionMap != null) {
            sessionMap.put("userSession", this);
        }
    }
    
    // Getters et Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public List<String> getRoles() {
        return new ArrayList<>(roles);
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }
    
    public long getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }
    
    public long getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    /**
     * Ajoute un attribut personnalisé
     */
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new java.util.HashMap<>();
        }
        attributes.put(key, value);
    }
    
    /**
     * Récupère un attribut personnalisé
     */
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }
    
    @Override
    public String toString() {
        return "UserSession{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", authenticated=" + authenticated +
                ", loginTime=" + loginTime +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
