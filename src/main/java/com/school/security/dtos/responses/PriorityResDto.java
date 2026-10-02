package com.school.security.dtos.responses;

/**
 * Référentiel des priorités de tâches ({@code GET /priorities}).
 *
 * <p>Reflète l'entité {@link com.school.security.entities.Priority}
 * ({@code priorityId}, {@code name}, {@code sortOrder}). L'ordre de tri
 * affiché est {@code sortOrder} croissant (1 = Urgente … 4 = Basse), jamais
 * {@code priorityId} (auto-incrément) qui ne doit pas être utilisé comme
 * règle d'ordre.
 */
public record PriorityResDto(
        Long priorityId,
        String name,
        Integer sortOrder
) {}