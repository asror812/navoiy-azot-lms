package org.example.navoiyazotlms.simple.repository;

import org.example.navoiyazotlms.simple.entity.OptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<OptionEntity, Long> {
    List<OptionEntity> findAllByQuestionId(Long questionId);
}
