package com.school.security.dtos.requests;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Optional;

/**
 * Désérialise {@code directionId} en distinguant les trois cas utiles :
 *
 * <ul>
 *   <li>champ absent : {@code null} — la direction existante n'est pas modifiée ;</li>
 *   <li>champ explicitement {@code null} : {@code Optional.empty()} — la direction
 *       est retirée ;</li>
 *   <li>champ numérique : {@code Optional.of(id)} — la direction est résolue et
 *       validée.</li>
 * </ul>
 */
public class OptionalDirectionIdDeserializer extends JsonDeserializer<Optional<Long>> {

    @Override
    public Optional<Long> deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return Optional.empty();
        }
        return Optional.ofNullable(context.readValue(parser, Long.class));
    }

    @Override
    public Optional<Long> getNullValue(DeserializationContext context) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getAbsentValue(DeserializationContext context) {
        return null;
    }
}
