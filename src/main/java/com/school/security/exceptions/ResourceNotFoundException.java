package com.school.security.exceptions;

/**
 * Exception signalant une ressource introuvable.
 *
 * <p>Rôle HTTP constaté : systématiquement transformée en réponse {@code 404}
 * par {@link GlobalExceptionHandler#handleResourceNotFoundException}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }
}