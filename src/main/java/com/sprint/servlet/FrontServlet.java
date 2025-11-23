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
            
            for (Class<?> controllerClass : controllerClasses) {
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                String controllerName = controllerClass.getSimpleName();
                
                System.out.println("Initialisation du contrôleur: " + controllerName);
                
                for (Method method : controllerClass.getDeclaredMethods()) {
                    Test testAnnotation = method.getAnnotation(Test.class);
                    if (testAnnotation != null) {
                        String path = testAnnotation.value();
                        routeMap.put(path, method);
                        pathPatterns.put(path, new PathPattern(path));
                        controllerInstances.put(method, controllerInstance);
                        System.out.println("Route enregistrée: " + path + " -> " + 
                            controllerName + "." + method.getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation des routes", e);
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
        Method method = trouverMethode(path);
        if (method != null) {
            try {
                method.setAccessible(true);
                Object controller = controllerInstances.get(method);
                
                // Extraire les paramètres du chemin
                Map<String, String> pathParams = extraireParametres(path);
                
                // Préparer les arguments pour la méthode
                Object[] args = prepareMethodArguments(method, req, resp, pathParams);
                
                // Appel de la méthode du contrôleur
                Object result = method.invoke(controller, args);
                
                // Traitement du résultat
                traiterResultat(result, req, resp);
                return true;
                
            } catch (Exception e) {
                gererErreur(e, resp);
            }
        }
        return false;
    }
    
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

    private Object convertToType(String value, Class<?> targetType) {
        if (value == null) return null;
        
        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class) 
                return Integer.parseInt(value);
            if (targetType == Long.class || targetType == long.class) 
                return Long.parseLong(value);
            // Ajoutez d'autres conversions au besoin
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Impossible de convertir la valeur en " + 
                                            targetType.getSimpleName(), e);
        }
        return value;
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


    private Method trouverMethode(String requestPath) {
        for (Map.Entry<String, PathPattern> entry : pathPatterns.entrySet()) {
            if (entry.getValue().matches(requestPath)) {
                return routeMap.get(entry.getKey());
            }
        }
        return null;
    }

    private Map<String, String> extraireParametres(String requestPath) {
        for (PathPattern pattern : pathPatterns.values()) {
            if (pattern.matches(requestPath)) {
                return pattern.extractParameters(requestPath);
            }
        }
        return Collections.emptyMap();
    }

    

    private Object[] prepareMethodArguments(Method method, HttpServletRequest req, 
                                        HttpServletResponse resp,
                                        Map<String, String> pathParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            Class<?> paramType = param.getType();
            
            // 1. Vérifier les paramètres de chemin
            if (pathParams.containsKey(paramName)) {
                args[i] = convertToType(pathParams.get(paramName), paramType);
            }
            // 2. Vérifier les paramètres de requête
            else if (req.getParameter(paramName) != null) {
                args[i] = convertToType(req.getParameter(paramName), paramType);
            }
            // 3. Vérifier les objets spéciaux (HttpServletRequest, HttpServletResponse)
            else if (paramType == HttpServletRequest.class) {
                args[i] = req;
            }
            else if (paramType == HttpServletResponse.class) {
                args[i] = resp;
            }
        }
        return args;
    }
}