package com.school.security.enums;

public enum RoleType {
    ADMIN,
    USER,
    SUPER_ADMIN;

    /**
     * Convertit un nom de rôle (chaîne) en enum, avec repli sur
     * {@code USER} pour les noms inconnus (rôles personnalisés) ou nuls.
     */
    public static RoleType fromNameOrUser(String name) {
        if (name == null || name.isBlank()) {
            return USER;
        }
        try {
            return RoleType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
