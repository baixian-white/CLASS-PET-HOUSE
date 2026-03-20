package com.classpethouse.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "score_rules")
public class ScoreRuleEntity extends BaseEntity {

    @JsonProperty("class_id")
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String icon = "⭐";

    @Column(nullable = false)
    private Integer value;

    @JsonProperty("sort_order")
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
