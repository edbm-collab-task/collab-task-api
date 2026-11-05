package com.school.security.dtos.responses;

/**
 * Action exposée à l'API : son code binaire, son nom technique et son libellé.
 *
 * <pre>
 * { "codeAction": 2, "name": "SELECT", "label": "Voir" }
 * </pre>
 *
 * @param codeAction code binaire de l'action (puissance de 2)
 * @param name nom technique (valeur de {@code ActionType})
 * @param label libellé affichable
 */
public record ActionResDto(Integer codeAction, String name, String label) {}
