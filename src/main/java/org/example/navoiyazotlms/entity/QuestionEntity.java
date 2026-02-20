package org.example.navoiyazotlms.simple.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simple_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private TestEntity test;

    @Column(nullable = false, columnDefinition = "text")
    private String text;
}
