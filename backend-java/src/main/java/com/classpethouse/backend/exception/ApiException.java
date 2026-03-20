package com.classpethouse.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, Object> extraBody;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Collections.emptyMap());
    }

    public ApiException(HttpStatus status, String message, Map<String, Object> extraBody) {
        super(message);
        this.status = status;
        this.extraBody = new LinkedHashMap<>(extraBody);
    }
}
