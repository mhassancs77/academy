package com.academy.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



@Component
public class SessionTimeoutInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        String requestURI = request.getRequestURI();

        // Skip session check for public pages
        if (isPublicPage(requestURI)) {
            return true;
        }

        // For AJAX requests, return JSON error instead of redirect
        boolean isAjaxRequest = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || requestURI.contains("-ajax");

        // Check if session exists and user is logged in
        if (session == null || (session.getAttribute("userId") == null && session.getAttribute("studentId") == null)) {
            if (isAjaxRequest) {
                // Return JSON error for AJAX requests
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"error\":\"الجلسة منتهية. يرجى تسجيل الدخول مرة أخرى\"}");
                return false;
            }

            // Check if it's a student trying to access student pages
            if (requestURI.startsWith("/student/")) {
                response.sendRedirect("/student/login?expired=true");
                return false;
            } else {
                // Teacher pages
                response.sendRedirect("/admin/login?expired=true");
                return false;
            }
        }

        return true;
    }

    private boolean isPublicPage(String uri) {
        return uri.equals("/") ||
                uri.equals("/admin/login") ||
                uri.equals("/register") ||
                uri.equals("/student/login") ||
                uri.startsWith("/teacher/") || // Portal pages
                uri.startsWith("/error") ||
                uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/images/");
    }
}