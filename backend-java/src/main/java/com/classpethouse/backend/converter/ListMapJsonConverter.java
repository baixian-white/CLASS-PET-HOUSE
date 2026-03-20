package com.classpethouse.backend.converter;

import com.classpethouse.backend.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Converter
public class ListMapJsonConverter implements AttributeConverter<List<Map<String, Object>>, String> {

    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> attribute) {
        return JsonUtils.toJson(attribute == null ? List.of() : attribute, "[]");
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String dbData) {
        return JsonUtils.fromJson(dbData, new TypeReference<>() {
        }, ArrayList::new);
    }
}
