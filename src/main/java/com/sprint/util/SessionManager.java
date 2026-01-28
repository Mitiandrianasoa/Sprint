package com.sprint.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de session basé sur Map au lieu de HttpSession
 * Permet de stocker des données de session sans dépendre de HttpSession
 */
public class SessionManager {
    
    // Stockage global des sessions par ID de session
    private static final Map<String, Map<String, Object>> sessionStore = new ConcurrentHashMap<>();
    
    // Nom de l'attribut pour stocker l'ID de session dans la requête
    private static final String SESSION_ID_ATTR = "SESSION_ID";
    
    // Durée de vie par défaut (30 minutes en millisecondes)
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000;
    
    // Map pour suivre les timestamps de création
    private static final Map<String, Long> sessionTimestamps = new ConcurrentHashMap<>();
    
    /**
     * Récupère ou crée une session pour la requête donnée
     * @param req La requête HTTP
     *param sessionName Nom de la session (pour multiple sessions)
     * @param create Si true, crée une nouvelle session si elle n'existe pas
     * @return Une Map représentant la session
     */
    public static Map<String, Object> getSession(HttpServletRequest req, String sessionName, boolean create) {
        // Récupérer ou générer l'ID de session
        String sessionId = getOrCreateSessionId(req);
        
        // Récupérer la session principale
        Map<String, Object> mainSession = sessionStore.computeIfAbsent(sessionId, k -> new HashMap<>());
        
        // Si un nom de session spécifique est demandé
        if (!"default".equals(sessionName)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> namedSession = (Map<String, Object>) mainSession.computeIfAbsent(
                sessionName, k -> new HashMap<>()
            );
            return namedSession;
        }
        
        return mainSession;
    }
    
    /**
     * Récupère une session existante sans en créer de nouvelle
     */
    public static Map<String, Object> getSession(HttpServletRequest req, String sessionName) {
        return getSession(req, sessionName, false);
    }
    
    /**
     * Récupère la session par défaut
     */
    public static Map<String, Object> getSession(HttpServletRequest req) {
        return getSession(req, "default", true);
    }
    
    /**
     * Récupère ou génère l'ID de session depuis la requête
     */
    private static String getOrCreateSessionId(HttpServletRequest req) {
        // Essayer de récupérer l'ID de session depuis les attributs de requête
        String sessionId = (String) req.getAttribute(SESSION_ID_ATTR);
        
        if (sessionId == null) {
            // Essayer de récupérer depuis les cookies ou paramètres
            sessionId = req.getParameter("sessionId");
            
            if (sessionId == null) {
                // Générer un nouvel ID de session
                sessionId = generateSessionId();
            }
            
            // Stocker dans les attributs de requête pour utilisation ultérieure
            req.setAttribute(SESSION_ID_ATTR, sessionId);
            
            // Mettre à jour le timestamp
            sessionTimestamps.put(sessionId, System.currentTimeMillis());
        }
        
        return sessionId;
    }
    
    /**
     * Génère un nouvel ID de session unique
     */
    private static String generateSessionId() {
        return "SESSION_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
    
    /**
     * Invalide une session spécifique
     */
    public static void invalidateSession(HttpServletRequest req) {
        String sessionId = (String) req.getAttribute(SESSION_ID_ATTR);
        if (sessionId != null) {
            sessionStore.remove(sessionId);
            sessionTimestamps.remove(sessionId);
            req.removeAttribute(SESSION_ID_ATTR);
        }
    }
    
    /**
     * Nettoie les sessions expirées
     */
    public static void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessionTimestamps.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > DEFAULT_TIMEOUT) {
                sessionStore.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Ajoute un attribut à la session
     */
    public static void setAttribute(HttpServletRequest req, String name, Object value) {
        Map<String, Object> session = getSession(req);
        session.put(name, value);
    }
    
    /**
     * Récupère un attribut de la session
     */
    public static Object getAttribute(HttpServletRequest req, String name) {
        Map<String, Object> session = getSession(req, "default", false);
        return session != null ? session.get(name) : null;
    }
    
    /**
     * Supprime un attribut de la session
     */
    public static void removeAttribute(HttpServletRequest req, String name) {
        Map<String, Object> session = getSession(req, "default", false);
        if (session != null) {
            session.remove(name);
        }
    }
    
    /**
     * Vérifie si la session contient un attribut
     */
    public static boolean hasAttribute(HttpServletRequest req, String name) {
        Map<String, Object> session = getSession(req, "default", false);
        return session != null && session.containsKey(name);
    }
    
    /**
     * Retourne l'ID de session actuel
     */
    public static String getSessionId(HttpServletRequest req) {
        return getOrCreateSessionId(req);
    }
    
    /**
     * Copie toutes les données de la session dans les attributs de requête
     * pour les rendre disponibles dans la vue JSP
     */
    public static void copyToRequestAttributes(HttpServletRequest req) {
        Map<String, Object> session = getSession(req, "default", false);
        if (session != null) {
            for (Map.Entry<String, Object> entry : session.entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }
        }
    }
}
