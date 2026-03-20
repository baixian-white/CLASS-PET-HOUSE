package com.classpethouse.backend.util;

import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.util.Map;

public final class AuthSupport {

    private AuthSupport() {
    }

    public static UserEntity currentUser(HttpServletRequest request) {
        Object user = request.getAttribute(AuthInterceptor.CURRENT_USER);
        if (user instanceof UserEntity userEntity) {
            return userEntity;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }

    public static void requireActivated(UserEntity user) {
        if (!Boolean.TRUE.equals(user.getIsActivated())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号未激活", Map.of("status", "not_activated"));
        }
    }
}
