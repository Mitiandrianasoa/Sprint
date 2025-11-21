package com.sprint;

import java.io.IOException;
<<<<<<< Updated upstream
import java.io.PrintWriter;
=======
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
>>>>>>> Stashed changes

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {
    
    // Map simplifiée : Route -> Méthode
    private Map<String, Method> routeMap = new HashMap<>();
    // Map pour stocker les instances de contrôleurs
    private Map<String, Object> controllerInstances = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        initialiserRoutes();
    }
    
    private void initialiserRoutes() throws ServletException {
        try {
            List<Class<?>> controllerClasses = PackageScanner.getClasses("com.sprint.controller");
            List<Class<?>> annotationClasses = PackageScanner.getClasses("com.sprint.annotation");
            
            // Lister les annotations détectées
            for (Class<?> annotationClass : annotationClasses) {
                if (annotationClass.isAnnotation()) {
                    System.out.println(annotationClass.getSimpleName());
                }
            }
            
            for (Class<?> controllerClass : controllerClasses) {
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                String controllerName = controllerClass.getSimpleName();
                
                System.out.println(controllerName);
                
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
            System.out.println("Route: " + path + " [@" + annotationClass.getSimpleName() + "] -> " + 
                            controllerName + "." + method.getName());
        } catch (Exception e) {
            System.err.println("Erreur avec l'annotation " + annotationClass.getSimpleName() + " sur " + 
                            controllerName + "." + method.getName() + ": " + e.getMessage());
        }
    }

    // Fonction qui scanne automatiquement le package annotation
    private void listerAnnotations() {
        try {
            System.out.println("=== ANNOTATIONS DISPONIBLES ===");
            List<Class<?>> annotationClasses = PackageScanner.getClasses("com.sprint.annotation");
            
            for (int i = 0; i < annotationClasses.size(); i++) {
                Class<?> annotationClass = annotationClasses.get(i);
                if (annotationClass.isAnnotation()) {
                    System.out.println((i + 1) + ". @" + annotationClass.getSimpleName() + " - " + annotationClass.getName());
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
        
<<<<<<< Updated upstream
        String path = req.getRequestURI().substring(req.getContextPath().length());
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.print("sprint: "+ path);
=======
        if (resourceExists) {
            RequestDispatcher defaultHandler = getServletContext().getNamedDispatcher("default");
            defaultHandler.forward(req, resp);
        } else {
            if (executerRoute(path, req, resp)) {
                return;
            } else {
                RequestDispatcher dispatcher = req.getRequestDispatcher("/index.html");
                dispatcher.forward(req, resp);
            }
        }
    }
    
    private boolean executerRoute(String path, HttpServletRequest req, HttpServletResponse resp) {
        Method method = routeMap.get(path);
        Object controller = controllerInstances.get(path);
        
        if (method != null && controller != null) {
            try {
                method.setAccessible(true);
                
                if (method.getParameterCount() == 0) {
                    Object result = method.invoke(controller);
                    if (result != null) {
                        resp.getWriter().write(result.toString());
                    }
                } else {
                    method.invoke(controller, req, resp);
                }
                return true;
                
            } catch (Exception e) {
                try { 
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("Erreur: " + e.getMessage()); 
                } catch (IOException ex) {}
            }
        }
        return false;
>>>>>>> Stashed changes
    }
}