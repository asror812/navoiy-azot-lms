package org.example.lms.repository;

import org.example.lms.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRepository extends JpaRepository<TestEntity, Long> {
    List<TestEntity> findAllByActiveTrueOrderByIdDesc();
    List<TestEntity> findAllByActiveTrueAndProfessionIgnoreCaseOrderByIdDesc(String profession);
}
