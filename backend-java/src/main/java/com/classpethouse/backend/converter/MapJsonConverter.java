package com.classpethouse.backend.converter;

import com.classpethouse.backend.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class MapJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        return JsonUtils.toJson(attribute == null ? Map.of() : attribute, "{}");
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        return JsonUtils.fromJson(dbData, new TypeReference<>() {
        }, LinkedHashMap::new);
    }
}
