package com.school.security.exceptions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestionnaire global des exceptions REST.
 *
 * <p>Correspondances exception → HTTP constatées (documentées, non modifiées) :
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → 400, corps
 *       {@code {"message":"Validation failed","errors":[{"field","message"}...]}} ;</li>
 *   <li>{@link BadRequestException} → 400, corps {@code {"message": ...}} ;</li>
 *   <li>{@link EntityException} → 404, corps {@code {"message": ...}} ;</li>
 *   <li>{@link ResourceNotFoundException} → 404, corps {@code {"message": ...}} ;</li>
 *   <li>aucun gestionnaire générique : les autres exceptions retombent sur la
 *       gestion d'erreur par défaut de Spring (typiquement 500).</li>
 * </ul>
 *
 * <p>Point non évident : {@code EntityException} sert aussi à des refus
 * métier/autorisation (ex. "Vous ne pouvez modifier que vos propres
 * commentaires") qui sont donc renvoyés en 404, et les corps utilisent
 * {@code Map.of} qui lève une {@code NullPointerException} si le message de
 * l'exception est {@code null}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Convertit les erreurs de validation de bean en 400 avec le détail par
     * champ (ordre des champs conservé).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception) {

        List<Map<String, String>> errors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(this::toErrorMap)
                        .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Validation failed");
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    /** Requête invalide → 400. */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequestException(
            BadRequestException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    /** Entité métier → 404 (y compris certains refus métier, voir note de classe). */
    @ExceptionHandler(EntityException.class)
    public ResponseEntity<Map<String, String>> handleEntityException(EntityException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    /** Ressource introuvable → 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(
            ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    /** Transforme une erreur de champ en couple {@code {field, message}}. */
    private Map<String, String> toErrorMap(FieldError fieldError) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", fieldError.getDefaultMessage());
        return error;
    }
}
