package org.example.lms.repository;

import org.example.lms.entity.AttemptAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswerEntity, Long> {
    List<AttemptAnswerEntity> findAllByAttemptId(Long attemptId);

    Optional<AttemptAnswerEntity> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
