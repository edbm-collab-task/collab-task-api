package com.school.security.enums;

public enum RoleType {
    ADMIN,
    USER,
    SUPER_ADMIN;

    /** Convertit un nom (String) en RoleType ; renvoie USER si inconnu ou null. */
    public static RoleType fromNameOrUser(String name) {
        if (name == null || name.isBlank()) return USER;
        try {
            return RoleType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
