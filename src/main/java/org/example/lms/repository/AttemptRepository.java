package org.example.lms.repository;

import org.example.lms.entity.AttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<AttemptEntity, Long> {
    Optional<AttemptEntity> findByIdAndCandidateId(Long attemptId, Long candidateId);

    Optional<AttemptEntity> findTopByCandidateIdAndFinishedFalseOrderByStartedAtDesc(Long candidateId);

    long countByCandidateId(Long candidateId);

    List<AttemptEntity> findAllByCandidateIdOrderByStartedAtAsc(Long candidateId);

    @Query("select a from AttemptEntity a join fetch a.candidate c order by a.startedAt desc")
    List<AttemptEntity> findAllWithCandidateOrderByStartedAtDesc();
}
