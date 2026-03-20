package com.classpethouse.backend.config;

import com.classpethouse.backend.security.AuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/reset-password",
                        "/api/admin/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path petDir = resolvePetAssetsDir();
        String location = ensureTrailingSlash(petDir.toUri().toString());
        registry.addResourceHandler("/pet-images/**")
                .addResourceLocations(location);
        registry.addResourceHandler("/动物图片/**")
                .addResourceLocations(location);
    }

    private Path resolvePetAssetsDir() {
        Path start = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Set<Path> candidates = new LinkedHashSet<>();

        Path cursor = start;
        for (int i = 0; i < 6 && cursor != null; i++) {
            candidates.add(cursor.resolve("assets/pets").normalize());
            candidates.add(cursor.resolve("CLASS-PET-HOUSE/assets/pets").normalize());
            cursor = cursor.getParent();
        }

        List<Path> checked = new ArrayList<>(candidates);
        for (Path candidate : checked) {
            if (Files.isDirectory(candidate)) {
                log.info("Pet image directory resolved to {}", candidate);
                return candidate;
            }
        }

        Path fallback = start.resolve("../assets/pets").normalize();
        log.warn("Pet image directory not found from {}. Falling back to {}", start, fallback);
        return fallback;
    }

    private String ensureTrailingSlash(String location) {
        return location.endsWith("/") ? location : location + "/";
    }
}
