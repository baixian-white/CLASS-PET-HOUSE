package com.classpethouse.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "licenses")
public class LicenseEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @JsonProperty("is_used")
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @JsonProperty("used_by")
    @Column(name = "used_by")
    private Integer usedBy;

    @JsonProperty("used_at")
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Transient
    private String username;

    @JsonProperty("created_at")
    public LocalDateTime getCreated_at() {
        return getCreatedAt();
    }
}
