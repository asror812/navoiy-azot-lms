package org.example.navoiyazotlms.simple.repository;

import org.example.navoiyazotlms.simple.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findAllByTestId(Long testId);
}
