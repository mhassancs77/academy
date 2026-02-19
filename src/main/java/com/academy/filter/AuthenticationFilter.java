package com.academy.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Authentication Filter
 * Checks if user is logged in before accessing protected pages
 */
// @Component  // ← COMMENTED OUT - Filter is disabled for now
public class AuthenticationFilter implements Filter {

    // Pages that don't require authentication
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/",                // Landing page (index.html)
            "/admin/login",
            "/register",
            "/student/login",
            "/h2-console",      // H2 Console access
            "/css/",
            "/js/",
            "/images/",
            "/static/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // Allow public URLs
        if (isPublicUrl(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);

        // Check if accessing student pages
        if (requestURI.startsWith("/student/")) {
            // Must be logged in as student
            if (session == null || session.getAttribute("studentId") == null) {
                httpResponse.sendRedirect("/student/login");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Check if accessing teacher pages
        if (session == null || session.getAttribute("userId") == null) {
            httpResponse.sendRedirect("/admin/login");
            return;
        }

        // Prevent students from accessing teacher pages
        String userType = (String) session.getAttribute("userType");
        if ("student".equals(userType) && !requestURI.startsWith("/student/")) {
            httpResponse.sendRedirect("/student/videos");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicUrl(String url) {
        for (String publicUrl : PUBLIC_URLS) {
            if (url.startsWith(publicUrl)) {
                return true;
            }
        }
        return false;
    }
}