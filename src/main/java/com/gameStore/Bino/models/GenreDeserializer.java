package com.gameStore.Bino.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//The frontend sends genre as an array of strings, while the database stores it
//as a single comma-separated string (which the frontend splits on "," when reading).
//Accept both shapes on the way in.
public class GenreDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            List<String> genres = new ArrayList<>();
            node.forEach(element -> genres.add(element.asText().trim()));
            return String.join(",", genres);
        }

        return node.asText();
    }
}
