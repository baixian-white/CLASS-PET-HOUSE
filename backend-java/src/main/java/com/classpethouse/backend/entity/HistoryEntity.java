package com.classpethouse.backend.entity;

import com.classpethouse.backend.dto.IdNameDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "history")
public class HistoryEntity extends BaseEntity {

    @JsonProperty("class_id")
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @JsonProperty("student_id")
    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @JsonProperty("rule_id")
    @Column(name = "rule_id")
    private Integer ruleId;

    @JsonProperty("rule_name")
    @Column(name = "rule_name", length = 50)
    private String ruleName;

    @Column(nullable = false)
    private Integer value = 0;

    @Column(nullable = false, length = 20)
    private String type = "score";

    @JsonProperty("is_revoked")
    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    @Transient
    @JsonProperty("Student")
    private IdNameDto student;
}
