package com.sprint;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RechercheResource(req, resp);
    }
    
    private void RechercheResource(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean resourceExists = getServletContext().getResource(path) != null;
        
        if (resourceExists) {
            RequestDispatcher defaultHandler = getServletContext().getNamedDispatcher("default");
            defaultHandler.forward(req, resp);
        } else {
            if (verifierAnnotationEtExecuter(path, req, resp)) {
                return;
            } else {
                // resp.getWriter().println(path);
                RequestDispatcher dispatcher = req.getRequestDispatcher("/index.html");
                dispatcher.forward(req, resp);
            }
        }
    }
    
    private boolean verifierAnnotationEtExecuter(String path, HttpServletRequest req, HttpServletResponse resp) {
        try {
            MyController controller = new MyController();
            
            for (java.lang.reflect.Method method : MyController.class.getDeclaredMethods()) {
                Test annotation = method.getAnnotation(Test.class);
                if (annotation != null && path.equals(annotation.value())) {
                    method.setAccessible(true);
                    
                    if (method.getParameterCount() == 0) {
                        resp.getWriter().write(method.invoke(controller).toString());
                    } else {
                        method.invoke(controller, req, resp);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            try { resp.getWriter().write("Erreur"); } catch (IOException ex) {}
        }
        return false;
    }
    
    private void RechercheResource(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean resourceExists = getServletContext().getResource(path) != null;
        
        if (resourceExists) {
            RequestDispatcher defaultHandler = getServletContext().getNamedDispatcher("default");
            defaultHandler.forward(req, resp);
        } else {
            resp.getWriter().println(path);
        }
    }
}
