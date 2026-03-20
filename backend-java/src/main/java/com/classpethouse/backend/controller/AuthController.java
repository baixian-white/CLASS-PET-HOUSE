package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.LicenseEntity;
import com.classpethouse.backend.entity.ScoreRuleEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.ClassRepository;
import com.classpethouse.backend.repository.LicenseRepository;
import com.classpethouse.backend.repository.ScoreRuleRepository;
import com.classpethouse.backend.repository.UserRepository;
import com.classpethouse.backend.security.JwtService;
import com.classpethouse.backend.util.AuthSupport;
import com.classpethouse.backend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final LicenseRepository licenseRepository;
    private final ClassRepository classRepository;
    private final ScoreRuleRepository scoreRuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            LicenseRepository licenseRepository,
            ClassRepository classRepository,
            ScoreRuleRepository scoreRuleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.licenseRepository = licenseRepository;
        this.classRepository = classRepository;
        this.scoreRuleRepository = scoreRuleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class) // 核心！确保发生异常时数据库回滚
    public Map<String, Object> register(@RequestBody Map<String, Object> body) {
        String username = safeTrim(RequestUtils.string(body, "username"));
        String password = RequestUtils.string(body, "password");
        String activationCode = safeTrim(RequestUtils.string(body, "activationCode"));

        if (isBlank(username) || isBlank(password) || isBlank(activationCode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名、密码和激活码不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名长度需为3-20个字符");
        }
        if (password.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码至少6个字符");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
        LicenseEntity license = licenseRepository.findByCodeAndIsUsedFalse(activationCode)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "激活码无效或已被使用"));


        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActivationCode(activationCode);
        user.setIsActivated(true);
        userRepository.save(user);

        // 3. 执行原子更新操作（并发安全的防线）
        int updatedRows = licenseRepository.consumeLicense(
                activationCode,
                user.getId(),         // 刚才拿到的新用户 ID
                LocalDateTime.now()
        );
        // 4. 校验更新结果
        if (updatedRows == 0) {
            // 如果受影响行数为 0，说明要么 code 不存在，要么 isUsed 已经是 true 了
            // 抛出异常会直接触发 @Transactional 回滚，刚才保存的 User 也会被从数据库撤销！
            throw new ApiException(HttpStatus.BAD_REQUEST, "激活码无效或已被使用");
        }
        createDefaultClassAndRules(user.getId());

        return authSuccess(user, "authenticated");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String username = safeTrim(RequestUtils.string(body, "username"));
        String password = RequestUtils.string(body, "password");
        if (isBlank(username) || isBlank(password)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        return authSuccess(user, Boolean.TRUE.equals(user.getIsActivated()) ? "authenticated" : "not_activated");
    }

    @PostMapping("/activate")
    @Transactional
    public Map<String, Object> activate(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        String code = safeTrim(RequestUtils.string(body, "code"));
        if (isBlank(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请输入激活码");
        }
        if (Boolean.TRUE.equals(user.getIsActivated())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号已激活，无需重复操作");
        }

        LicenseEntity license = licenseRepository.findByCodeAndIsUsedFalse(code)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "激活码无效或已被使用"));

        license.setIsUsed(true);
        license.setUsedBy(user.getId());
        license.setUsedAt(LocalDateTime.now());
        licenseRepository.save(license);

        user.setActivationCode(code);
        user.setIsActivated(true);
        userRepository.save(user);
        createDefaultClassAndRules(user.getId());

        return Map.of(
                "message", "激活成功",
                "user", publicUser(user)
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        return Map.of("user", publicUserWithSettings(user));
    }

    @GetMapping("/check")
    public Map<String, Object> check(HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        return Map.of("status", Boolean.TRUE.equals(user.getIsActivated()) ? "authenticated" : "not_activated");
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        // 1. 获取当前用户
        UserEntity user = AuthSupport.currentUser(request);

        // 2. 清除数据库中的 Token，使其失效
        if (user != null) {
            user.setCurrentToken(null);
            userRepository.save(user);
        }
        return Map.of("message", "已退出");
    }

    @PutMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        String oldPassword = RequestUtils.string(body, "oldPassword");
        String newPassword = RequestUtils.string(body, "newPassword");
        if (isBlank(oldPassword) || isBlank(newPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写完整");
        }
        if (newPassword.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密码至少6个字符");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "旧密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Map.of("message", "密码修改成功");
    }

    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> body) {
        String username = safeTrim(RequestUtils.string(body, "username"));
        String activationCode = safeTrim(RequestUtils.string(body, "activationCode"));
        String newPassword = RequestUtils.string(body, "newPassword");

        if (isBlank(username) || isBlank(activationCode) || isBlank(newPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写完整");
        }
        if (newPassword.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密码至少6个字符");
        }

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "用户名或激活码不匹配"));
        if (!activationCode.equals(user.getActivationCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名或激活码不匹配");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Map.of("message", "密码重置成功");
    }

    @PutMapping("/settings")
    public Map<String, Object> updateSettings(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        Map<String, Object> settings = new LinkedHashMap<>();
        if (user.getSettings() != null) {
            settings.putAll(user.getSettings());
        }
        for (String key : List.of("theme", "sound", "animation", "language", "fontSize")) {
            if (body.containsKey(key)) {
                settings.put(key, body.get(key));
            }
        }
        user.setSettings(settings);
        userRepository.save(user);
        return Map.of(
                "message", "设置已保存",
                "settings", settings
        );
    }

    @PostMapping("/verify-password")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        String password = RequestUtils.string(body, "password");
        boolean valid = !isBlank(password) && passwordEncoder.matches(password, user.getPasswordHash());
        return Map.of("valid", valid);
    }

    private void createDefaultClassAndRules(Integer userId) {
        ClassEntity defaultClass = new ClassEntity();
        defaultClass.setUserId(userId);
        defaultClass.setName("默认班级");
        defaultClass.setSortOrder((int) classRepository.countByUserId(userId));
        classRepository.save(defaultClass);

        List<ScoreRuleEntity> rules = new ArrayList<>();
        rules.add(rule(defaultClass.getId(), "早读打卡", "📖", 1, 0));
        rules.add(rule(defaultClass.getId(), "作业优秀", "⭐", 3, 1));
        rules.add(rule(defaultClass.getId(), "课堂表现好", "🙋", 2, 2));
        rules.add(rule(defaultClass.getId(), "帮助同学", "🤝", 2, 3));
        rules.add(rule(defaultClass.getId(), "考试进步", "📈", 5, 4));
        rules.add(rule(defaultClass.getId(), "值日认真", "🧹", 1, 5));
        rules.add(rule(defaultClass.getId(), "运动达标", "🏃", 2, 6));
        rules.add(rule(defaultClass.getId(), "迟到", "⏰", -1, 7));
        rules.add(rule(defaultClass.getId(), "未交作业", "📝", -2, 8));
        rules.add(rule(defaultClass.getId(), "课堂违纪", "🚫", -2, 9));
        rules.add(rule(defaultClass.getId(), "打架", "👊", -5, 10));
        rules.add(rule(defaultClass.getId(), "说脏话", "🤐", -1, 11));
        rules.add(rule(defaultClass.getId(), "不守纪律", "⚠️", -1, 12));
        rules.add(rule(defaultClass.getId(), "损坏公物", "💔", -3, 13));
        scoreRuleRepository.saveAll(rules);
    }

    private ScoreRuleEntity rule(Integer classId, String name, String icon, int value, int sortOrder) {
        ScoreRuleEntity entity = new ScoreRuleEntity();
        entity.setClassId(classId);
        entity.setName(name);
        entity.setIcon(icon);
        entity.setValue(value);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private Map<String, Object> authSuccess(UserEntity user, String status) {
        // 1. 生成全新的 Token
        String token = jwtService.generateToken(user);

        // 2. 将新 Token 保存到该用户的数据库记录中
        user.setCurrentToken(token);
        userRepository.save(user);
        return Map.of(
                "token", token,
                "user", publicUser(user),
                "status", status
        );
    }

    private Map<String, Object> publicUser(UserEntity user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "is_activated", Boolean.TRUE.equals(user.getIsActivated())
        );
    }

    private Map<String, Object> publicUserWithSettings(UserEntity user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("is_activated", Boolean.TRUE.equals(user.getIsActivated()));
        data.put("settings", user.getSettings() == null ? Map.of() : user.getSettings());
        return data;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
