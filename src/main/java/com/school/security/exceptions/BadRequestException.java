package com.school.security.exceptions;

/**
 * Exception signalant une requête invalide ou des données manquantes.
 *
 * <p>Rôle HTTP constaté : systématiquement transformée en réponse {@code 400}
 * par {@link GlobalExceptionHandler#handleBadRequestException}. Utilisée pour
 * les erreurs de validation, paramètres manquants ou requête mal formulée.
 * <p>Point non évident : {@code Map.of("message", message)} lance une
 * {@code NullPointerException} si le message de l'exception est {@code null} ;
 * l'exception elle-même survit, mais le handler plante en retour HTTP.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }
}