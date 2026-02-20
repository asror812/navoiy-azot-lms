package org.example.navoiyazotlms.simple.repository;

import org.example.navoiyazotlms.simple.entity.CandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateRepository extends JpaRepository<CandidateEntity, Long> {
    Optional<CandidateEntity> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);
}
