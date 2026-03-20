package com.classpethouse.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @JsonProperty("activation_code")
    @Column(name = "activation_code", length = 100)
    private String activationCode;

    @JsonProperty("is_activated")
    @Column(name = "is_activated", nullable = false)
    private Boolean isActivated = false;

    @JsonIgnore
    @Column(name = "is_admin", nullable = false)
    private Boolean admin = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> settings = new LinkedHashMap<>();

    @JsonProperty("current_token")
    @Column(name = "current_token", length = 512) // 长度根据你的JWT长度调整
    private String currentToken;
}
