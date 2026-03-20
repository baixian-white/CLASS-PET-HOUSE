package com.classpethouse.backend.security;

import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = Optional.ofNullable(request.getHeader("Authorization")).orElse("");
        if (!authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        String token = authorization.substring(7).trim();
        try {
            Integer userId = jwtService.parseUserId(token);
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在"));
            // 单设备登录（互挤下线）校验逻辑
            if (user.getCurrentToken() == null || !user.getCurrentToken().equals(token)) {
                // 抛出 401，前端收到此提示后应清除本地 Token 并跳回登录页
                throw new ApiException(HttpStatus.UNAUTHORIZED, "您的账号已在其他设备登录，请重新登录");
            }
            request.setAttribute(CURRENT_USER, user);
            return true;
        } catch (ExpiredJwtException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录已过期");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "认证失败");
        }
    }
}
