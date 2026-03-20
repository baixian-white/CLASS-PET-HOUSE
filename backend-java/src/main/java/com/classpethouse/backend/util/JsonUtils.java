package com.classpethouse.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object value, String fallback) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    public static <T> T fromJson(String value, TypeReference<T> typeReference, Supplier<T> fallbackSupplier) {
        if (value == null || value.isBlank()) {
            return fallbackSupplier.get();
        }
        try {
            return OBJECT_MAPPER.readValue(value, typeReference);
        } catch (Exception ex) {
            return fallbackSupplier.get();
        }
    }
}
