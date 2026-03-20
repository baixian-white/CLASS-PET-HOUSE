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
@Table(name = "exchange_records")
public class ExchangeRecordEntity extends BaseEntity {

    @JsonProperty("class_id")
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @JsonProperty("student_id")
    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @JsonProperty("item_id")
    @Column(name = "item_id")
    private Integer itemId;

    @JsonProperty("item_name")
    @Column(name = "item_name", nullable = false, length = 50)
    private String itemName;

    @Column(nullable = false)
    private Integer cost;

    @Transient
    @JsonProperty("Student")
    private IdNameDto student;
}
