package org.example.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attempt_answers", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "attempt_id", "question_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AttemptEntity attempt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private OptionEntity selectedOption;

    @Column(nullable = false)
    private Boolean correct;
}
