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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "students")
public class StudentEntity extends BaseEntity {

    @JsonProperty("class_id")
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonProperty("pet_type")
    @Column(name = "pet_type", length = 50)
    private String petType;

    @JsonProperty("pet_name")
    @Column(name = "pet_name", length = 50)
    private String petName;

    @JsonProperty("food_count")
    @Column(name = "food_count")
    private Integer foodCount = 0;

    @JsonProperty("food_reset_at")
    @Column(name = "food_reset_at")
    private LocalDateTime foodResetAt;

    @JsonProperty("total_food_earned")
    @Column(name = "total_food_earned", nullable = false)
    private Integer totalFoodEarned = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> badges = new ArrayList<>();

    @JsonProperty("current_badges_count")
    @Column(name = "current_badges_count", nullable = false)
    private Integer currentBadgesCount = 0;

    @JsonProperty("total_badges_earned")
    @Column(name = "total_badges_earned", nullable = false)
    private Integer totalBadgesEarned = 0;

    @JsonProperty("sort_order")
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @JsonProperty("group_id")
    @Column(name = "group_id")
    private Integer groupId;

    @Transient
    @JsonProperty("Group")
    private IdNameDto group;
}
