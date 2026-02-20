package org.example.navoiyazotlms.simple.repository;

import org.example.navoiyazotlms.simple.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRepository extends JpaRepository<TestEntity, Long> {
    List<TestEntity> findAllByActiveTrueOrderByIdDesc();
}
