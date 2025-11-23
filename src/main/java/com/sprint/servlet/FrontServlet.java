package com.sprint.servlet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sprint.model.ModelView;
import com.sprint.util.PackageScanner;

@WebServlet("/*")
public class FrontServlet extends HttpServlet {
    private Map<String, Method> routeMap = new HashMap<>();
    private Map<String, Object> controllerInstances = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        initialiserRoutes();
        listerAnnotations();
    }
    
    private void initialiserRoutes() throws ServletException {
        try {
            List<Class<?>> controllerClasses = PackageScanner.getClasses("com.sprint.controller");
            List<Class<?>> annotationClasses = PackageScanner.getClasses("com.sprint.annotation");
            
            for (Class<?> controllerClass : controllerClasses) {
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                String controllerName = controllerClass.getSimpleName();
                
                System.out.println("Initialisation du contrôleur: " + controllerName);
                
                for (Method method : controllerClass.getDeclaredMethods()) {
                    for (Class<?> annotationClass : annotationClasses) {
                        if (annotationClass.isAnnotation()) {
                            Annotation annotation = method.getAnnotation((Class<? extends Annotation>) annotationClass);
                            if (annotation != null) {
                                enregistrerRoute(annotation, annotationClass, method, controllerInstance, controllerName);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation des routes: " + e.getMessage(), e);
        }
    }

    private void enregistrerRoute(Annotation annotation, Class<?> annotationClass, Method method, 
                                Object controllerInstance, String controllerName) {
        try {
            String path = (String) annotationClass.getMethod("value").invoke(annotation);
            routeMap.put(path, method);
            controllerInstances.put(path, controllerInstance);
            System.out.println("Route enregistrée: " + path + " [@" + 
                annotationClass.getSimpleName() + "] -> " + controllerName + "." + method.getName());
        } catch (Exception e) {
            System.err.println("Erreur avec l'annotation " + annotationClass.getSimpleName() + 
                " sur " + controllerName + "." + method.getName() + ": " + e.getMessage());
        }
    }

    private void listerAnnotations() {
        try {
            System.out.println("=== ANNOTATIONS DISPONIBLES ===");
            List<Class<?>> annotationClasses = PackageScanner.getClasses("com.sprint.annotation");
            
            for (int i = 0; i < annotationClasses.size(); i++) {
                Class<?> annotationClass = annotationClasses.get(i);
                if (annotationClass.isAnnotation()) {
                    System.out.println((i + 1) + ". @" + annotationClass.getSimpleName() + 
                        " - " + annotationClass.getName());
                }
            }
            System.out.println("===============================");
            
        } catch (Exception e) {
            System.out.println("Erreur lors du scan des annotations: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        traiterRequete(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        traiterRequete(req, resp);
    }

    private void traiterRequete(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        // Vérifier si c'est une ressource statique
        if (estRessourceStatique(path)) {
            RequestDispatcher defaultHandler = getServletContext().getNamedDispatcher("default");
            defaultHandler.forward(req, resp);
            return;
        }
        
        // Sinon, essayer d'exécuter la route
        if (!executerRoute(path, req, resp)) {
            // Aucune route trouvée, rediriger vers la page d'accueil
            RequestDispatcher dispatcher = req.getRequestDispatcher("/index.html");
            dispatcher.forward(req, resp);
        }
    }
    
    private boolean estRessourceStatique(String path) {
        return path.startsWith("/static/") || 
               path.endsWith(".css") || 
               path.endsWith(".js") || 
               path.endsWith(".png") || 
               path.endsWith(".jpg") || 
               path.endsWith(".jpeg") || 
               path.endsWith(".gif") ||
               path.endsWith(".ico");
    }
    
    private boolean executerRoute(String path, HttpServletRequest req, HttpServletResponse resp) {
        Method method = routeMap.get(path);
        Object controller = controllerInstances.get(path);
        
        if (method != null && controller != null) {
            try {
                method.setAccessible(true);
                
                // Appel de la méthode du contrôleur
                Object result = method.invoke(controller, getParametresMethode(method, req, resp));
                
                // Traitement du résultat
                traiterResultat(result, req, resp);
                return true;
                
            } catch (Exception e) {
                gererErreur(e, resp);
            }
        }
        return false;
    }
    
    // private Object[] getParametresMethode(Method method, HttpServletRequest req, HttpServletResponse resp) {
    //     // Implémentez la logique pour extraire les paramètres de la requête
    //     // et les convertir dans le type attendu par la méthode
    //     return new Object[0];
    // }
    
    private void traiterResultat(Object result, HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        if (result == null) {
            return;
        }
        
        if (result instanceof String) {
            // Retour texte simple
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write((String) result);
            
        } else if (result instanceof ModelView) {
            // Gestion du ModelView
            ModelView modelView = (ModelView) result;
            
            // Transfert des données vers les attributs de requête
            if (modelView.getData() != null) {
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            
            // Redirection vers la vue
            String viewPath = "/WEB-INF/views/" + modelView.getView() + ".jsp";
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
            dispatcher.forward(req, resp);
        }
        // Ajoutez d'autres types de retours si nécessaire
    }
    
    private void gererErreur(Exception e, HttpServletResponse resp) {
        try {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("Erreur lors du traitement de la requête: " + e.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}