package com.sprint;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String path = req.getRequestURI().substring(req.getContextPath().length());
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.print("sprint: "+ path);
    }
}