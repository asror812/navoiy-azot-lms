package org.example.navoiyazotlms.simple.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simple_attempt_questions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "attempt_id", "question_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AttemptEntity attempt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;
}
