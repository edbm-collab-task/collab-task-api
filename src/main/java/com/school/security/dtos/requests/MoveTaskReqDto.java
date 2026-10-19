package com.school.security.dtos.requests;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête de déplacement (drag & drop) d'une tâche
 * ({@code PATCH /tasks/{taskId}/move}).
 *
 * @param statusId      statut cible (requis)
 * @param beforeTaskId  carte de référence optionnelle : la tâche s'insère juste
 *                      avant elle dans son groupe (statut + priorité) et adopte
 *                      sa priorité. {@code null} = la tâche garde sa priorité et
 *                      rejoint la fin de son groupe dans le nouveau statut.
 */
public record MoveTaskReqDto(
        @NotNull(message = "Status is required") Long statusId,
        Long beforeTaskId) {}