package org.example.lms.repository;

import org.example.lms.entity.AttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttemptRepository extends JpaRepository<AttemptEntity, Long> {
    boolean existsByCandidateId(Long candidateId);

    Optional<AttemptEntity> findByIdAndCandidateId(Long attemptId, Long candidateId);
}
