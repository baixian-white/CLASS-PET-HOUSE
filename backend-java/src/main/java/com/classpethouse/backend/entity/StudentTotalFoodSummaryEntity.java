package com.classpethouse.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 记录学生历史上累计获得的正向食物分构成。
 * 这张表只统计“已生效且未撤回”的正向积分规则，方便做总食物排行榜和构成展示。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "student_total_food_summaries",
        uniqueConstraints = @UniqueConstraint(name = "uq_student_total_food_summaries_student_rule", columnNames = {"class_id", "student_id", "rule_id"})
)
public class StudentTotalFoodSummaryEntity extends BaseEntity {

    @JsonProperty("class_id")
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @JsonProperty("student_id")
    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @JsonProperty("rule_id")
    @Column(name = "rule_id", nullable = false)
    private Integer ruleId;

    @JsonProperty("rule_name")
    @Column(name = "rule_name", nullable = false, length = 50)
    private String ruleName;

    @JsonProperty("award_count")
    @Column(name = "award_count", nullable = false)
    private Integer awardCount = 0;

    @JsonProperty("total_food")
    @Column(name = "total_food", nullable = false)
    private Integer totalFood = 0;
}
