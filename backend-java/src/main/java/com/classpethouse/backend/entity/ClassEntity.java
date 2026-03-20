package com.classpethouse.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "classes")
public class ClassEntity extends BaseEntity {

    @JsonProperty("user_id")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 100)
    private String name = "默认班级";

    @JsonProperty("system_name")
    @Column(name = "system_name", length = 100)
    private String systemName = "石榴果宠物屋";

    @Column(length = 50)
    private String theme = "pink";

    @JsonProperty("growth_stages")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "growth_stages", columnDefinition = "jsonb")
    private List<Integer> growthStages = List.of(0, 5, 10, 20, 30, 45, 60, 75, 90, 100);

    @JsonProperty("sort_order")
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
