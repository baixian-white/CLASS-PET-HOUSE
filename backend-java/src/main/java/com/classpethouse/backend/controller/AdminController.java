package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.LicenseEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.LicenseRepository;
import com.classpethouse.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(LicenseRepository licenseRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.licenseRepository = licenseRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/licenses/generate")
    @Transactional
    public Map<String, Object> generateLicenses(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        authorize(request);
        Integer count = body.get("count") instanceof Number number ? number.intValue() : 1;
        int safeCount = Math.max(1, Math.min(count, 100));
        List<String> codes = new java.util.ArrayList<>();
        for (int i = 0; i < safeCount; i++) {
            String code = "CPH-" + randomHex(16);
            LicenseEntity license = new LicenseEntity();
            license.setCode(code);
            licenseRepository.save(license);
            codes.add(code);
        }
        return Map.of(
                "message", "生成" + codes.size() + "个卡密",
                "codes", codes
        );
    }

    @GetMapping("/licenses")
    public List<LicenseEntity> licenses(HttpServletRequest request) {
        authorize(request);
        List<LicenseEntity> licenses = licenseRepository.findAllByOrderByCreatedAtDesc();
        Map<Integer, String> users = new LinkedHashMap<>();
        for (UserEntity user : userRepository.findAll()) {
            users.put(user.getId(), user.getUsername());
        }
        licenses.forEach(license -> license.setUsername(license.getUsedBy() == null ? null : users.getOrDefault(license.getUsedBy(), "未知账户")));
        return licenses;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(HttpServletRequest request) {
        authorize(request);
        return userRepository.findAll().stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("is_activated", Boolean.TRUE.equals(user.getIsActivated()));
            item.put("is_admin", Boolean.TRUE.equals(user.getAdmin()));
            item.put("created_at", user.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(HttpServletRequest request) {
        authorize(request);
        return Map.of(
                "totalUsers", userRepository.count(),
                "activatedUsers", userRepository.countByIsActivatedTrue(),
                "totalLicenses", licenseRepository.count(),
                "usedLicenses", licenseRepository.countByIsUsedTrue()
        );
    }

    @GetMapping("/debug-env")
    public Map<String, Object> debugEnv() {
        return Map.of(
                "configured", userRepository.countByAdminTrue() > 0,
                "admin_count", userRepository.countByAdminTrue(),
                "source", "database",
                "cwd", System.getProperty("user.dir"),
                "node_env", System.getProperty("spring.profiles.active", "")
        );
    }

    private void authorize(HttpServletRequest request) {
        if (userRepository.countByAdminTrue() <= 0) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "管理员账号未初始化");
        }

        AdminCredentials credentials = extractCredentials(request);
        if (credentials == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "管理员认证失败");
        }

        UserEntity admin = userRepository.findByUsernameAndAdminTrue(credentials.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "管理员认证失败"));
        if (!passwordEncoder.matches(credentials.password(), admin.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "管理员认证失败");
        }
    }

    private AdminCredentials extractCredentials(HttpServletRequest request) {
        String authHeader = header(request, "Authorization");
        if (authHeader.startsWith("Basic ")) {
            try {
                String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
                int sep = decoded.indexOf(':');
                String username = sep >= 0 ? decoded.substring(0, sep).trim() : decoded.trim();
                String password = sep >= 0 ? decoded.substring(sep + 1).trim() : "";
                if (!username.isBlank() && !password.isBlank()) {
                    return new AdminCredentials(username, password);
                }
            } catch (Exception ignored) {
            }
        }

        String username = valueOrEmpty(request.getHeader("X-Admin-Username"), request.getHeader("username")).trim();
        String password = valueOrEmpty(request.getHeader("X-Admin-Password"), request.getHeader("password")).trim();
        if (username.isBlank() || password.isBlank()) {
            return null;
        }
        return new AdminCredentials(username, password);
    }

    private record AdminCredentials(String username, String password) {
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value;
    }

    private String valueOrEmpty(String primary, String secondary) {
        return primary != null && !primary.isBlank() ? primary : (secondary == null ? "" : secondary);
    }

    private String randomHex(int length) {
        byte[] bytes = new byte[length / 2];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }
}
