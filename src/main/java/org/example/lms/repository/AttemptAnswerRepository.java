package org.example.lms.repository;

import org.example.lms.entity.AttemptAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswerEntity, Long> {
}
