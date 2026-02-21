package org.example.lms.repository;

import org.example.lms.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findAllByActiveTrueOrderByIdDesc();

    List<QuestionEntity> findAllByActiveTrueAndProfessionIgnoreCaseOrderByIdDesc(String profession);

    long countByProfessionIgnoreCase(String profession);
}
