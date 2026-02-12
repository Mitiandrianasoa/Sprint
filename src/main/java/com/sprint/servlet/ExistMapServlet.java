package com.sprint.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import com.sprint.model.UserSession;
import com.sprint.util.SessionManager;

/**
 * Servlet pour gérer les sessions basées sur Map
 * Vérifie l'existence de données de session et les rend disponibles dans la vue
 */
@WebServlet("/exist-map")
public class ExistMapServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Récupérer la session Map
        Map<String, Object> session = SessionManager.getSession(req);
        
        // Vérifier si la session contient des données utilisateur
        UserSession userSession = UserSession.fromSessionMap(session);
        
        // Copier toutes les données de session dans les attributs de requête
        // pour les rendre disponibles dans la vue JSP
        SessionManager.copyToRequestAttributes(req);
        
        // Ajouter des informations spécifiques sur la session
        req.setAttribute("sessionId", SessionManager.getSessionId(req));
        req.setAttribute("sessionExists", !session.isEmpty());
        req.setAttribute("sessionSize", session.size());
        req.setAttribute("userAuthenticated", userSession != null && userSession.isAuthenticated());
        
        if (userSession != null) {
            req.setAttribute("currentUser", userSession);
            req.setAttribute("userRoles", userSession.getRoles());
            req.setAttribute("loginTime", userSession.getLoginTime());
            req.setAttribute("lastActivity", userSession.getLastActivity());
        }
        
        // Rediriger vers la vue d'affichage
        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/views/exist-map.jsp");
        dispatcher.forward(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Récupérer la session
        Map<String, Object> session = SessionManager.getSession(req);
        
        // Récupérer les paramètres du formulaire
        String action = req.getParameter("action");
        
        if ("createUser".equals(action)) {
            // Créer une nouvelle session utilisateur
            String username = req.getParameter("username");
            String email = req.getParameter("email");
            String userId = "USER_" + System.currentTimeMillis();
            
            UserSession userSession = new UserSession(userId, username, email);
            
            // Ajouter des rôles par défaut
            String[] roles = req.getParameterValues("roles");
            if (roles != null) {
                for (String role : roles) {
                    userSession.addRole(role);
                }
            }
            
            // Authentifier l'utilisateur
            userSession.authenticate(userSession.getRoles());
            
            // Sauvegarder dans la session
            userSession.saveToSessionMap(session);
            
            // Ajouter un message de succès
            session.put("message", "Utilisateur créé avec succès: " + username);
            
        } else if ("logout".equals(action)) {
            // Déconnecter l'utilisateur
            UserSession userSession = UserSession.fromSessionMap(session);
            if (userSession != null) {
                userSession.logout();
                session.remove("userSession");
                session.put("message", "Utilisateur déconnecté avec succès");
            }
            
        } else if ("addData".equals(action)) {
            // Ajouter des données générales à la session
            String key = req.getParameter("key");
            String value = req.getParameter("value");
            
            if (key != null && !key.trim().isEmpty()) {
                session.put(key, value);
                session.put("message", "Donnée ajoutée à la session: " + key + " = " + value);
            }
        }
        
        // Rediriger vers GET pour afficher le résultat
        resp.sendRedirect(req.getContextPath() + "/exist-map");
    }
}
