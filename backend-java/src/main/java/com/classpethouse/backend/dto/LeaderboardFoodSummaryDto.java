package com.classpethouse.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 总食物排行榜中的“分数组成”明细。
 */
public record LeaderboardFoodSummaryDto(
        @JsonProperty("rule_id") Integer ruleId,
        @JsonProperty("rule_name") String ruleName,
        @JsonProperty("award_count") Integer awardCount,
        @JsonProperty("total_food") Integer totalFood
) {
}
