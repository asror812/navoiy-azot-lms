package org.example.lms.entity;

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

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String profession;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false, columnDefinition = "text")
    private String text;
}
