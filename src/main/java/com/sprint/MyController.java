package com.sprint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyController {

    @Test("/hello")
    public String hello() {
        return "Hello from hello()";
    }

    @Test("/greet")
    public void greet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("Greetings from greet()");
    }
}