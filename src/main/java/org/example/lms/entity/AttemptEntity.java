package org.example.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempts", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "candidate_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private CandidateEntity candidate;

    @Column(nullable = false)
    private String profession;

    @Column(nullable = false)
    private Boolean finished;

    @Column(nullable = false)
    private Integer totalQuestions;

    private Integer correctAnswers;
    private Double score;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
