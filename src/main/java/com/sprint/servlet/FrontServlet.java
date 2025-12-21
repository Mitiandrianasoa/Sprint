package com.sprint.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sprint.annotation.Test;
import com.sprint.annotation.RequestParam; // Ajouter cet import
import com.sprint.model.ModelView;
import com.sprint.util.PackageScanner;
import com.sprint.util.PathPattern;

@WebServlet("/")
public class FrontServlet extends HttpServlet {
    private Map<String, Method> routeMap = new HashMap<>();
    private Map<Method, Object> controllerInstances = new HashMap<>();
    private Map<String, PathPattern> pathPatterns = new HashMap<>();

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
                // Création d'une instance du contrôleur
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                
                // Parcours des méthodes du contrôleur
                for (Method method : controllerClass.getDeclaredMethods()) {
                    // Extraction de la méthode HTTP et du chemin
                    String httpMethod = extractHttpMethod(method);
                    String path = extractPath(method);
                    
                    // Si un chemin valide est trouvé
                    if (path != null && !path.isEmpty()) {
                        // Création de la clé de routage (ex: "GET:/users" ou "/endpoint")
                        String key = httpMethod != null ? httpMethod + ":" + path : path;
                        
                        // Enregistrement de la route
                        routeMap.put(key, method);
                        controllerInstances.put(method, controllerInstance);
                        
                        // Gestion des paramètres d'URL
                        if (path.contains("{")) {
                            pathPatterns.put(key, new PathPattern(path));
                        }
                        
                        // Log pour le débogage
                        System.out.println("Route enregistrée: " + key + " -> " + 
                                        controllerClass.getSimpleName() + "." + method.getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation des routes", e);
        }
    }

    // Méthode utilitaire pour extraire la méthode HTTP d'une annotation
    private String extractHttpMethod(Method method) {
        Get get = method.getAnnotation(Get.class);
        if (get != null) return "GET";
        
        Post post = method.getAnnotation(Post.class);
        if (post != null) return "POST";
        
        Test test = method.getAnnotation(Test.class);
        if (test != null && test.method().length > 0) {
            return test.method()[0];
        }
        
        return null;
    }

    // Méthode utilitaire pour extraire le chemin d'une annotation
    private String extractPath(Method method) {
        Get get = method.getAnnotation(Get.class);
        if (get != null) return get.value();
        
        Post post = method.getAnnotation(Post.class);
        if (post != null) return post.value();
        
        Test test = method.getAnnotation(Test.class);
        if (test != null) return test.value();
        
        return null;
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

        if (estRessourceStatique(path)) {
            RequestDispatcher defaultHandler = getServletContext().getNamedDispatcher("default");
            if (defaultHandler != null) {
                defaultHandler.forward(req, resp);
            }
            return;
        }

        if (!executerRoute(path, req, resp)) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write("<h1>404 - Page non trouvée</h1><p>Route: " + path + "</p>");
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
                path.endsWith(".ico") ||
                path.endsWith(".html");
    }

    private boolean executerRoute(String path, HttpServletRequest req, HttpServletResponse resp) {
        Method method = trouverMethode(path);
        if (method == null) {
            return false;
        }
        try {
            // 1. Récupérer l'instance du contrôleur
            Object controller = controllerInstances.computeIfAbsent(
                method, 
                key -> {
                    try {
                        return key.getDeclaringClass().getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Erreur lors de la création du contrôleur", e);
                    }
                }
            );

            // 2. Extraire les arguments
            Object[] args = extraireArguments(method, path, req, resp);

            // 3. Appeler la méthode du contrôleur
            Object result = method.invoke(controller, args);

            // 4. Traiter le résultat
            traiterResultat(result, req, resp);
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'exécution de la route: " + path, e);
        }
    }


    private void traiterResultat(Object result, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (result == null) {
            return;
        }

        if (result instanceof String) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write((String) result);

        } else if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;

            // NOUVEAU Sprint 8: Sauvegarder le type de données détecté
            req.setAttribute("dataType", modelView.getDataType());

            // Transférer toutes les données du ModelView vers la requête
            if (modelView.getData() != null) {
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            // Forward vers la vue JSP
            String viewPath = "/WEB-INF/views/" + modelView.getView() + ".jsp";
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
            dispatcher.forward(req, resp);
        }
        else {
            // Pour les autres types d'objets, on les ajoute comme attribut 
            // avec le nom de la classe en minuscules
            String attributeName = result.getClass().getSimpleName();
            attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
            req.setAttribute(attributeName, result);
        }
    }

    private Object convertToType(String value, Class<?> targetType) {
        if (value == null) return null;

        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class)
                return Integer.parseInt(value);
            if (targetType == Long.class || targetType == long.class)
                return Long.parseLong(value);
            if (targetType == Double.class || targetType == double.class)
                return Double.parseDouble(value);
            if (targetType == Boolean.class || targetType == boolean.class)
                return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Impossible de convertir '" + value + "' en " +
                    targetType.getSimpleName(), e);
        }
        return value;
    }

    private void gererErreur(Exception e, HttpServletResponse resp) {
        try {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write("<h1>Erreur 500</h1><pre>" + e.getMessage() + "</pre>");
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

    private Object[] extraireArguments(Method method, String path, HttpServletRequest req, HttpServletResponse resp) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        // Récupérer le pattern de l'URL depuis l'annotation @Test
        String urlPattern = method.getAnnotation(Test.class).value();
        Map<String, String> pathParams = extraireParametresChemin(urlPattern, path);
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            try {
                // 1. Vérifier si c'est un paramètre de requête avec @RequestParam
                if (param.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = param.getAnnotation(RequestParam.class);
                    String paramName = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
                    
                    // Vérifier d'abord dans les paramètres de chemin, puis dans les paramètres de requête
                    String paramValue = pathParams.getOrDefault(paramName, req.getParameter(paramName));
                    
                    if (paramValue == null && requestParam.required()) {
                        throw new IllegalArgumentException("Paramètre requis manquant: " + paramName);
                    }
                    
                    args[i] = convertToType(paramValue, param.getType());
                }
                // 2. Vérifier si c'est un paramètre de chemin (nom doit correspondre)
                else if (pathParams.containsKey(param.getName())) {
                    args[i] = convertToType(pathParams.get(param.getName()), param.getType());
                }
                // 3. Injection des objets spéciaux
                else if (param.getType() == HttpServletRequest.class) {
                    args[i] = req;
                } 
                else if (param.getType() == HttpServletResponse.class) {
                    args[i] = resp;
                }
                // 4. Gestion des paramètres de requête sans annotation (par nom de paramètre)
                else {
                    String paramValue = req.getParameter(param.getName());
                    if (paramValue != null) {
                        args[i] = convertToType(paramValue, param.getType());
                    } else if (param.getType().isPrimitive()) {
                        // Valeur par défaut pour les types primitifs
                        args[i] = getDefaultValue(param.getType());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'extraction de l'argument " + 
                                        param.getName() + ": " + e.getMessage(), e);
            }
        }
        
        return args;
    }

    // Méthode utilitaire pour extraire les paramètres de chemin
    private Map<String, String> extraireParametresChemin(String pattern, String path) {
        Map<String, String> params = new HashMap<>();
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        
        if (patternParts.length != pathParts.length) {
            return params;
        }
        
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                String paramName = patternPart.substring(1, patternPart.length() - 1);
                params.put(paramName, pathParts[i]);
            }
        }
        
        return params;
    }

    // Méthode utilitaire pour obtenir une valeur par défaut pour les types primitifs
    private Object getDefaultValue(Class<?> type) {
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == double.class) return 0.0;
            if (type == float.class) return 0.0f;
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == char.class) return '\0';
            return null;
    }


}

