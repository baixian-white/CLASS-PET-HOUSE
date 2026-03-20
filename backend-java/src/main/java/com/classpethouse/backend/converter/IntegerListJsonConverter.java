package com.classpethouse.backend.converter;

import com.classpethouse.backend.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class IntegerListJsonConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        return JsonUtils.toJson(attribute == null ? List.of() : attribute, "[]");
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        return JsonUtils.fromJson(dbData, new TypeReference<>() {
        }, ArrayList::new);
    }
}
