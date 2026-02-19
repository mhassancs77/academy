package com.academy.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to convert grade level values to Arabic names
 */
public class GradeLevelUtil {

    private static final Map<String, String> GRADE_NAMES = new HashMap<>();

    static {
        // Number format
        GRADE_NAMES.put("1", "الصف الأول");
        GRADE_NAMES.put("2", "الصف الثاني");
        GRADE_NAMES.put("3", "الصف الثالث");
        GRADE_NAMES.put("4", "الصف الرابع");
        GRADE_NAMES.put("5", "الصف الخامس");
        GRADE_NAMES.put("6", "الصف السادس");

        // Already in Arabic format (return as-is)
        GRADE_NAMES.put("الصف الأول", "الصف الأول");
        GRADE_NAMES.put("الصف الثاني", "الصف الثاني");
        GRADE_NAMES.put("الصف الثالث", "الصف الثالث");
        GRADE_NAMES.put("الصف الرابع", "الصف الرابع");
        GRADE_NAMES.put("الصف الخامس", "الصف الخامس");
        GRADE_NAMES.put("الصف السادس", "الصف السادس");
    }

    /**
     * Convert grade level value to Arabic name
     * @param gradeLevel The grade level (e.g., "1" or "الصف الأول")
     * @return Arabic grade name (e.g., "الصف الأول")
     */
    public static String getGradeName(String gradeLevel) {
        if (gradeLevel == null || gradeLevel.trim().isEmpty()) {
            return "غير محدد";
        }

        return GRADE_NAMES.getOrDefault(gradeLevel.trim(), gradeLevel);
    }

    /**
     * Get all grade options for dropdowns
     */
    public static Map<String, String> getAllGrades() {
        Map<String, String> grades = new HashMap<>();
        grades.put("الصف الأول", "الصف الأول");
        grades.put("الصف الثاني", "الصف الثاني");
        grades.put("الصف الثالث", "الصف الثالث");
        grades.put("الصف الرابع", "الصف الرابع");
        grades.put("الصف الخامس", "الصف الخامس");
        grades.put("الصف السادس", "الصف السادس");
        return grades;
    }
}