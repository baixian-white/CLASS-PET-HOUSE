package com.classpethouse.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 排行榜单条学生数据。
 * rankValue 会随着不同榜单切换到对应统计字段，前端无需自己判断取哪个字段做展示。
 */
public record LeaderboardStudentItemDto(
        Integer id,
        String name,
        @JsonProperty("pet_type") String petType,
        @JsonProperty("pet_name") String petName,
        @JsonProperty("group_id") Integer groupId,
        @JsonProperty("Group") IdNameDto group,
        @JsonProperty("food_count") Integer foodCount,
        @JsonProperty("total_food_earned") Integer totalFoodEarned,
        @JsonProperty("current_badges_count") Integer currentBadgesCount,
        @JsonProperty("total_badges_earned") Integer totalBadgesEarned,
        @JsonProperty("rank_value") Integer rankValue,
        @JsonProperty("food_summary") List<LeaderboardFoodSummaryDto> foodSummary
) {
}
