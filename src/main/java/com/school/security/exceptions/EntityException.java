package com.school.security.exceptions;

/**
 * Exception métier signalant une entité introuvable ou un refus d'opération.
 *
 * <p>Rôle HTTP constaté : systématiquement transformée en réponse {@code 404}
 * par {@link GlobalExceptionHandler#handleEntityException}. Remarque : cette
 * exception sert également aux refus métier/autorisation (ex. "Vous ne pouvez
 * modifier que vos propres commentaires") ; ceux-ci aboutissent donc en 404,
 * ce qui est discutable d'un point de vue HTTP.
 * <p>Point non évident : comme {@code BadRequestException}, {@code Map.of("message",
 * message)} peut lever {@code NullPointerException} si le message est {@code null}.
 */
public class EntityException extends RuntimeException {

    public EntityException() {}

    public EntityException(String message) {
        super(message);
    }
}
