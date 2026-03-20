package com.classpethouse.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 四类排行榜统一响应，前端切榜时只需要拉取一次接口数据。
 */
public record ClassLeaderboardsDto(
        @JsonProperty("current_food_ranking") List<LeaderboardStudentItemDto> currentFoodRanking,
        @JsonProperty("total_food_ranking") List<LeaderboardStudentItemDto> totalFoodRanking,
        @JsonProperty("current_badges_ranking") List<LeaderboardStudentItemDto> currentBadgesRanking,
        @JsonProperty("total_badges_ranking") List<LeaderboardStudentItemDto> totalBadgesRanking
) {
}
