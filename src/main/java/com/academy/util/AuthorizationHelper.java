package com.academy.util;


import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Authorization Helper
 * Provides methods to check user permissions
 */
@Component
public class AuthorizationHelper {

    /**
     * Check if user is logged in
     */
    public static boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("userId") != null;
    }

    /**
     * Check if student is logged in
     */
    public static boolean isStudentAuthenticated(HttpSession session) {
        return session.getAttribute("studentId") != null;
    }

    /**
     * Get current teacher ID
     */
    public static Long getCurrentTeacherId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    /**
     * Get current student ID
     */
    public static Long getCurrentStudentId(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    /**
     * Check if teacher owns the resource
     * Prevents teacher from accessing another teacher's data
     */
    public static boolean canAccessResource(HttpSession session, Long resourceTeacherId) {
        Long currentTeacherId = getCurrentTeacherId(session);
        return currentTeacherId != null && currentTeacherId.equals(resourceTeacherId);
    }

    /**
     * Check if student can access the video
     * Student can only access videos from their teacher
     */
    public static boolean canStudentAccessVideo(HttpSession session, Long videoTeacherId) {
        Long studentTeacherId = (Long) session.getAttribute("teacherId");
        return studentTeacherId != null && studentTeacherId.equals(videoTeacherId);
    }

    /**
     * Redirect URL for unauthorized access
     */
    public static String getUnauthorizedRedirect(HttpSession session) {
        if (isStudentAuthenticated(session)) {
            return "redirect:/student/videos";
        } else if (isAuthenticated(session)) {
            return "redirect:/dashboard";
        } else {
            return "redirect:/admin/login";
        }
    }

}
