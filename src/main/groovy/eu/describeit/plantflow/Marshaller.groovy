package eu.describeit.plantflow

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import groovy.transform.CompileStatic

/**
 * Utility class for JSON marshalling and unmarshalling using Jackson.
 */
@CompileStatic
class Marshaller {
    
    /**
     * Marshals an object to JSON string.
     *
     * @param object The object to marshal
     * @param pretty Whether to pretty-print the JSON or not
     * @return JSON string representation of the object
     * @throws JsonProcessingException if the object cannot be serialized
     */
    static String toJson(Object object, boolean pretty = false) throws JsonProcessingException {
        final String json
        if (pretty) json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object)
        else        json = OBJECT_MAPPER.writeValueAsString(object)

        return json
    }
    
    /**
     * Unmarshals a JSON string to an object of the specified type.
     *
     * @param json The JSON string to unmarshall
     * @param type The class type to deserialize into
     * @param <T> The target type
     * @return The deserialized object
     * @throws JsonProcessingException if the JSON cannot be deserialized
     */
    static <T> T fromJson(String json, Class<T> type) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, type)
    }

    private static final ObjectMapper OBJECT_MAPPER = initObjectMapper()

    private static ObjectMapper initObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
//        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        return mapper
    }
}
