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
        if (method != null) {
            try {
                method.setAccessible(true);
                Object controller = controllerInstances.get(method);

                Map<String, String> pathParams = extraireParametres(path);
                Object[] args = prepareMethodArguments(method, req, resp, pathParams);
                Object result = method.invoke(controller, args);

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
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write((String) result);

        } else if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;

            if (modelView.getData() != null) {
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            String viewPath = "/WEB-INF/views/" + modelView.getView() + ".jsp";
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
            dispatcher.forward(req, resp);
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

    private Object[] prepareMethodArguments(Method method, HttpServletRequest req,
                                            HttpServletResponse resp,
                                            Map<String, String> pathParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            Class<?> paramType = param.getType();

            if (pathParams.containsKey(paramName)) {
                args[i] = convertToType(pathParams.get(paramName), paramType);
            }
            else if (req.getParameter(paramName) != null) {
                args[i] = convertToType(req.getParameter(paramName), paramType);
            }
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