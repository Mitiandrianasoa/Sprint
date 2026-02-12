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
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.annotation.Test;
import com.sprint.annotation.Get;
import com.sprint.annotation.Post;
import com.sprint.annotation.RestController;
import com.sprint.annotation.ResponseBody;
import com.sprint.annotation.RequestParam;
import com.sprint.annotation.Session;
import com.sprint.model.ModelView;
import com.sprint.model.JsonResponse;
import com.sprint.model.MultipartFile;
import com.sprint.util.PackageScanner;
import com.sprint.util.PathPattern;
import com.sprint.util.EntityBinder;
import com.sprint.util.MultipartRequestHandler;
import com.sprint.util.SessionManager;
import com.sprint.security.SecurityInterceptor;
import com.sprint.model.UserSession;

@WebServlet("/")
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10MB max file size
    maxRequestSize = 1024 * 1024 * 50,   // 50MB max request size
    fileSizeThreshold = 1024 * 1024      // 1MB memory threshold
)
public class FrontServlet extends HttpServlet {
    private Map<String, Method> routeMap = new HashMap<>();
    private Map<Method, Object> controllerInstances = new HashMap<>();
    private Map<String, PathPattern> pathPatterns = new HashMap<>();
    private Map<Class<?>, Boolean> restControllerCache = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                // Vérifier si c'est un contrôleur REST
                boolean isRestController = controllerClass.isAnnotationPresent(RestController.class);
                restControllerCache.put(controllerClass, isRestController);
                
                String controllerPrefix = "";
                if (isRestController) {
                    RestController restController = controllerClass.getAnnotation(RestController.class);
                    controllerPrefix = restController.value();
                }
                
                // Création d'une instance du contrôleur
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                
                // Parcours des méthodes du contrôleur
                for (Method method : controllerClass.getDeclaredMethods()) {
                    // Extraction de la méthode HTTP et du chemin
                    String httpMethod = extractHttpMethod(method);
                    String path = extractPath(method);
                    
                    // Si un chemin valide est trouvé
                    if (path != null && !path.isEmpty()) {
                        // Ajouter le préfixe du contrôleur REST si présent
                        String fullPath = controllerPrefix + path;
                        
                        // Création de la clé de routage (ex: "GET:/users" ou "/endpoint")
                        String key = httpMethod != null ? httpMethod + ":" + fullPath : fullPath;
                        
                        // Enregistrement de la route
                        routeMap.put(key, method);
                        controllerInstances.put(method, controllerInstance);
                        
                        // Gestion des paramètres d'URL
                        if (fullPath.contains("{")) {
                            pathPatterns.put(key, new PathPattern(fullPath));
                        }
                        
                        // Log pour le débogage
                        System.out.println("Route enregistrée: " + key + " -> " + 
                                        controllerClass.getSimpleName() + "." + method.getName() +
                                        (isRestController ? " [REST]" : ""));
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
            // Retourner une erreur 404 en JSON si la route n'est pas trouvée
            resp.setContentType("application/json;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonResponse errorResponse = JsonResponse.notFound("Route non trouvée: " + path);
            resp.getWriter().write(errorResponse.toJson());
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

            // 2. Récupérer la session pour la vérification de sécurité
            Map<String, Object> session = SessionManager.getSession(req);
            UserSession userSession = UserSession.fromSessionMap(session);

            // 3. Vérification de sécurité (Sprint 11 bis)
            Object securityResult = SecurityInterceptor.checkSecurity(method, userSession, session, req, resp);
            if (securityResult != null) {
                // La sécurité a bloqué l'accès, traiter le résultat de sécurité
                traiterResultat(securityResult, req, resp, method, controller);
                return true;
            }

            // 4. Extraire les arguments (avec support des fichiers)
            Object[] args = extraireArguments(method, path, req, resp);

            // 5. Appeler la méthode du contrôleur
            Object result = method.invoke(controller, args);

            // 6. Traiter le résultat avec les informations du contrôleur
            traiterResultat(result, req, resp, method, controller);
            return true;

        } catch (Exception e) {
            gererErreurJson(e, resp);
            return false;
        }
    }

    private void traiterResultat(Object result, HttpServletRequest req, HttpServletResponse resp,
                                 Method method, Object controller)
            throws ServletException, IOException {
        if (result == null) {
            // Si le résultat est null, retourner une réponse JSON vide
            if (estRetourJson(method, controller)) {
                sendJsonResponse(JsonResponse.success(), resp);
            }
            return;
        }

        // Vérifier si on doit retourner du JSON
        if (estRetourJson(method, controller) || result instanceof JsonResponse) {
            // Retourner du JSON
            sendJsonResponse(result, resp);
        } else if (result instanceof String) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write((String) result);
        } else if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            
            // Sauvegarder le type de données détecté
            req.setAttribute("dataType", modelView.getDataType());
            
            // Transférer toutes les données du ModelView vers la requête
            if (modelView.getData() != null) {
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            
            // Copier les données de session dans les attributs de requête pour la vue
            SessionManager.copyToRequestAttributes(req);
            
            // Forward vers la vue JSP
            String viewPath = "/WEB-INF/views/" + modelView.getView() + ".jsp";
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
            dispatcher.forward(req, resp);
        } else {
            // Pour les autres types d'objets non-REST, on les ajoute comme attribut
            String attributeName = result.getClass().getSimpleName();
            attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
            req.setAttribute(attributeName, result);
            
            // Par défaut, forward vers une vue JSP basée sur le nom de la méthode
            String viewName = method.getName();
            String viewPath = "/WEB-INF/views/" + viewName + ".jsp";
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
            if (dispatcher != null) {
                dispatcher.forward(req, resp);
            } else {
                // Si la vue n'existe pas, retourner l'objet en JSON par défaut
                sendJsonResponse(result, resp);
            }
        }
    }

    private boolean estRetourJson(Method method, Object controller) {
        // Vérifier si c'est un contrôleur REST
        boolean isRestController = restControllerCache.getOrDefault(controller.getClass(), false);
        
        // Vérifier si la méthode a l'annotation @ResponseBody
        boolean hasResponseBody = method.isAnnotationPresent(ResponseBody.class);
        
        // Vérifier si la méthode retourne un type qui devrait être en JSON
        Class<?> returnType = method.getReturnType();
        boolean returnsJsonResponse = returnType == JsonResponse.class;
        boolean returnsObject = !EntityBinder.isSimpleType(returnType) && 
                              !returnType.isPrimitive() && 
                              returnType != String.class && 
                              returnType != ModelView.class &&
                              returnType != MultipartFile.class;
        
        return isRestController || hasResponseBody || returnsJsonResponse || returnsObject;
    }

    private void sendJsonResponse(Object result, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        
        if (result instanceof JsonResponse) {
            // Si c'est déjà un JsonResponse
            JsonResponse jsonResponse = (JsonResponse) result;
            resp.setStatus(jsonResponse.getCode());
            resp.getWriter().write(jsonResponse.toJson());
        } else {
            // Sinon, encapsuler dans un JsonResponse
            JsonResponse jsonResponse = JsonResponse.success(result);
            resp.setStatus(200);
            resp.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        }
    }

    private void gererErreurJson(Exception e, HttpServletResponse resp) {
        try {
            e.printStackTrace();
            resp.setContentType("application/json;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            JsonResponse errorResponse = JsonResponse.serverError(e.getMessage());
            resp.getWriter().write(errorResponse.toJson());
        } catch (IOException ex) {
            ex.printStackTrace();
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

    private Method trouverMethode(String requestPath) {
        String httpMethod = "GET"; // Par défaut
        String key = httpMethod + ":" + requestPath;
        
        if (routeMap.containsKey(key)) {
            return routeMap.get(key);
        }
        
        // Vérifier les patterns avec paramètres
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
        Test testAnnotation = method.getAnnotation(Test.class);
        String urlPattern = testAnnotation != null ? testAnnotation.value() : "";
        Map<String, String> pathParams = extraireParametresChemin(urlPattern, path);
        
        // Vérifier si c'est une requête multipart
        boolean isMultipartRequest = MultipartRequestHandler.isMultipartRequest(req);
        
        // Extraire les fichiers si c'est une requête multipart
        Map<String, MultipartFile> multipartFiles = new HashMap<>();
        if (isMultipartRequest) {
            try {
                multipartFiles = MultipartRequestHandler.extractMultipartFiles(req);
                System.out.println("📁 Requête multipart détectée. Fichiers: " + multipartFiles.size());
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'extraction des fichiers: " + e.getMessage());
            }
        }
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            try {
                // CAS SPRINT 10: Gestion des fichiers MultipartFile
                if (paramType == MultipartFile.class) {
                    if (isMultipartRequest) {
                        // Rechercher le fichier par le nom du paramètre
                        String paramName = param.getName();
                        if (param.isAnnotationPresent(RequestParam.class)) {
                            RequestParam requestParam = param.getAnnotation(RequestParam.class);
                            paramName = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
                        }
                        
                        args[i] = multipartFiles.get(paramName);
                        if (args[i] != null) {
                            System.out.println("✅ Fichier bindé: " + paramName + " -> " + 
                                            ((MultipartFile) args[i]).getOriginalFilename());
                        }
                    }
                    continue;
                }
                
                if (EntityBinder.isEntity(paramType)) {
                    System.out.println("🔍 Entity détectée: " + paramType.getSimpleName());
                    args[i] = EntityBinder.bindEntity(req, paramType);
                    System.out.println("✅ Entity bindée: " + args[i]);
                    continue;
                }
                
                if (param.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = param.getAnnotation(RequestParam.class);
                    String paramName = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
                    
                    String paramValue = null;
                    
                    // Vérifier d'abord dans les paramètres de chemin
                    if (pathParams.containsKey(paramName)) {
                        paramValue = pathParams.get(paramName);
                    }
                    // Sinon vérifier dans les paramètres de requête
                    else if (isMultipartRequest) {
                        // Pour les requêtes multipart, extraire les paramètres textuels
                        try {
                            Map<String, String> multipartParams = MultipartRequestHandler.extractMultipartParameters(req);
                            paramValue = multipartParams.get(paramName);
                        } catch (Exception e) {
                            paramValue = req.getParameter(paramName);
                        }
                    } else {
                        paramValue = req.getParameter(paramName);
                    }
                    
                    if (paramValue == null && requestParam.required()) {
                        throw new IllegalArgumentException("Paramètre requis manquant: " + paramName);
                    }
                    
                    args[i] = convertToType(paramValue, paramType);
                }
                // 2. Vérifier si c'est un paramètre de chemin (nom doit correspondre)
                else if (pathParams.containsKey(param.getName())) {
                    args[i] = convertToType(pathParams.get(param.getName()), paramType);
                }
                // 3. Injection des objets spéciaux
                else if (paramType == HttpServletRequest.class) {
                    args[i] = req;
                } 
                else if (paramType == HttpServletResponse.class) {
                    args[i] = resp;
                }
                // 4. Gestion des paramètres de requête sans annotation (par nom de paramètre)
                else if (EntityBinder.isSimpleType(paramType)) {
                    String paramValue = null;
                    
                    if (isMultipartRequest) {
                        try {
                            Map<String, String> multipartParams = MultipartRequestHandler.extractMultipartParameters(req);
                            paramValue = multipartParams.get(param.getName());
                        } catch (Exception e) {
                            paramValue = req.getParameter(param.getName());
                        }
                    } else {
                        paramValue = req.getParameter(param.getName());
                    }
                    
                    if (paramValue != null) {
                        args[i] = convertToType(paramValue, paramType);
                    } else if (paramType.isPrimitive()) {
                        // Valeur par défaut pour les types primitifs
                        args[i] = getDefaultValue(paramType);
                    }
                }
                // 5. CAS SPRINT 11: Gestion des sessions Map avec @Session
                else if (param.isAnnotationPresent(Session.class) || paramType == Map.class) {
                    Session sessionAnnotation = param.getAnnotation(Session.class);
                    String sessionName = "default";
                    boolean create = true;
                    
                    if (sessionAnnotation != null) {
                        sessionName = sessionAnnotation.value();
                        create = sessionAnnotation.create();
                    }
                    
                    args[i] = SessionManager.getSession(req, sessionName, create);
                    System.out.println(" Session Map bindée: " + sessionName + " -> " + args[i]);
                }
                // 5. Si aucun cas ne correspond et que ce n'est pas une entity
                else {
                    args[i] = null;
                }
                
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'extraction de l'argument " + 
                                param.getName() + ": " + e.getMessage());
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
}