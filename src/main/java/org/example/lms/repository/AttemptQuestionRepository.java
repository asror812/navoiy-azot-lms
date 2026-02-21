package org.example.lms.repository;

import org.example.lms.entity.AttemptQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestionEntity, Long> {
    List<AttemptQuestionEntity> findAllByAttemptId(Long attemptId);

    List<AttemptQuestionEntity> findAllByAttemptIdOrderByDisplayOrderAsc(Long attemptId);

    boolean existsByQuestionId(Long questionId);
}
