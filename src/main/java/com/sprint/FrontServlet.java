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
            resp.getWriter().println(path);
        }
    }
}
