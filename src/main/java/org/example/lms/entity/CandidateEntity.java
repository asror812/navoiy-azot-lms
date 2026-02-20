package org.example.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simple_candidates", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "login" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String login;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean active;
}
