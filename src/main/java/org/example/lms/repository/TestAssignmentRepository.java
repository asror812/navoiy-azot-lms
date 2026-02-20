package org.example.lms.repository;

import org.example.lms.entity.TestAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAssignmentRepository extends JpaRepository<TestAssignmentEntity, Long> {
    List<TestAssignmentEntity> findAllByCandidateIdAndActiveTrue(Long candidateId);

    Optional<TestAssignmentEntity> findByCandidateIdAndTestIdAndActiveTrue(Long candidateId, Long testId);

    boolean existsByCandidateIdAndTestId(Long candidateId, Long testId);
}
