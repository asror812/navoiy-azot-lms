package org.example.lms.repository;

import org.example.lms.entity.OptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<OptionEntity, Long> {
    List<OptionEntity> findAllByQuestionId(Long questionId);
}
