package com.classpethouse.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "groups")
public class GroupEntity extends BaseEntity {

    @JsonProperty("class_id")
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonProperty("sort_order")
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Transient
    private List<StudentEntity> students = new ArrayList<>();
}
