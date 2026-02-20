package org.example.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simple_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Integer minQuestionsPerAttempt;

    @Column(nullable = false)
    private Integer maxQuestionsPerAttempt;

    @Column(nullable = false)
    private String createdBy;
}
