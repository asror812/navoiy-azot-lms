package org.example.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.dto.HrDtos;
import org.example.lms.entity.*;
import org.example.lms.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HrService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final CandidateRepository candidateRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TestEntity> listTests() {
        return testRepository.findAll();
    }

    public List<CandidateEntity> listCandidates() {
        return candidateRepository.findAll();
    }

    public TestEntity createTest(HrDtos.CreateTestRequest req, String hrUsername) {
        int min = req.minQuestionsPerAttempt() == null ? 30 : req.minQuestionsPerAttempt();
        int max = req.maxQuestionsPerAttempt() == null ? 40 : req.maxQuestionsPerAttempt();

        if (min < 1 || max < min) {
            throw new IllegalArgumentException("Invalid question range");
        }

        TestEntity entity = TestEntity.builder()
                .title(req.title())
                .active(req.active() == null || req.active())
                .minQuestionsPerAttempt(min)
                .maxQuestionsPerAttempt(max)
                .createdBy(hrUsername)
                .build();

        TestEntity saved = testRepository.save(entity);
        log.info("HR {} created test id={} title={}", hrUsername, saved.getId(), saved.getTitle());
        return saved;
    }

    public TestEntity updateTest(Long id, HrDtos.UpdateTestRequest req) {
        TestEntity test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));

        if (req.title() != null && !req.title().isBlank()) {
            test.setTitle(req.title());
        }
        if (req.active() != null) {
            test.setActive(req.active());
        }
        if (req.minQuestionsPerAttempt() != null) {
            test.setMinQuestionsPerAttempt(req.minQuestionsPerAttempt());
        }
        if (req.maxQuestionsPerAttempt() != null) {
            test.setMaxQuestionsPerAttempt(req.maxQuestionsPerAttempt());
        }
        if (test.getMaxQuestionsPerAttempt() < test.getMinQuestionsPerAttempt()) {
            throw new IllegalArgumentException("Invalid question range");
        }

        TestEntity updated = testRepository.save(test);
        log.info("Test updated id={} title={}", updated.getId(), updated.getTitle());
        return updated;
    }

    public void deleteTest(Long id) {
        testRepository.deleteById(id);
        log.info("Test deleted id={}", id);
    }

    public CandidateEntity createCandidate(HrDtos.CreateCandidateRequest req) {
        if (candidateRepository.existsByLoginIgnoreCase(req.login())) {
            throw new IllegalArgumentException("Candidate login already exists");
        }
        CandidateEntity saved = candidateRepository.save(CandidateEntity.builder()
                .fullName(req.fullName())
                .login(req.login().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .active(req.active() == null || req.active())
                .build());
        log.info("Candidate created id={} login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    public CandidateEntity updateCandidate(Long candidateId, HrDtos.UpdateCandidateRequest req) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        if (req.fullName() != null && !req.fullName().isBlank()) {
            candidate.setFullName(req.fullName());
        }
        if (req.password() != null && !req.password().isBlank()) {
            candidate.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        if (req.active() != null) {
            candidate.setActive(req.active());
        }

        CandidateEntity updated = candidateRepository.save(candidate);
        log.info("Candidate updated id={} login={}", updated.getId(), updated.getLogin());
        return updated;
    }

    public void deleteCandidate(Long candidateId) {
        candidateRepository.deleteById(candidateId);
        log.info("Candidate deleted id={}", candidateId);
    }

    public TestAssignmentEntity assignTest(Long candidateId, Long testId) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        TestAssignmentEntity assignment = testAssignmentRepository
                .findByCandidateIdAndTestIdAndActiveTrue(candidateId, testId)
                .orElseGet(() -> TestAssignmentEntity.builder()
                        .candidate(candidate)
                        .test(test)
                        .active(true)
                        .assignedAt(LocalDateTime.now())
                        .build());

        assignment.setActive(true);
        assignment.setAssignedAt(LocalDateTime.now());
        TestAssignmentEntity saved = testAssignmentRepository.save(assignment);
        log.info("Test assigned candidateId={} testId={}", candidateId, testId);
        return saved;
    }

    public void unassignTest(Long candidateId, Long testId) {
        TestAssignmentEntity assignment = testAssignmentRepository
                .findByCandidateIdAndTestIdAndActiveTrue(candidateId, testId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        assignment.setActive(false);
        testAssignmentRepository.save(assignment);
        log.info("Test unassigned candidateId={} testId={}", candidateId, testId);
    }

    @Transactional
    public QuestionEntity addQuestion(Long testId, HrDtos.CreateQuestionRequest req) {
        TestEntity test = testRepository.findById(testId).orElseThrow(() -> new IllegalArgumentException("Test not found"));

        long correctCount = req.options().stream().filter(HrDtos.OptionRequest::correct).count();
        if (correctCount != 1) {
            throw new IllegalArgumentException("Exactly one option must be correct");
        }

        QuestionEntity question = questionRepository.save(QuestionEntity.builder()
                .test(test)
                .text(req.text())
                .build());

        List<OptionEntity> options = req.options().stream()
                .map(o -> OptionEntity.builder()
                        .question(question)
                        .text(o.text())
                        .correct(o.correct())
                        .build())
                .toList();

        optionRepository.saveAll(options);
        log.info("Question created id={} testId={}", question.getId(), testId);
        return question;
    }

    @Transactional
    public QuestionEntity updateQuestion(Long questionId, HrDtos.UpdateQuestionRequest req) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        if (req.text() != null && !req.text().isBlank()) {
            question.setText(req.text());
        }
        questionRepository.save(question);

        if (req.options() != null && !req.options().isEmpty()) {
            long correctCount = req.options().stream().filter(HrDtos.OptionRequest::correct).count();
            if (correctCount != 1) {
                throw new IllegalArgumentException("Exactly one option must be correct");
            }
            List<OptionEntity> oldOptions = optionRepository.findAllByQuestionId(questionId);
            optionRepository.deleteAll(oldOptions);
            optionRepository.saveAll(req.options().stream()
                    .map(o -> OptionEntity.builder()
                            .question(question)
                            .text(o.text())
                            .correct(o.correct())
                            .build())
                    .toList());
        }

        log.info("Question updated id={}", questionId);
        return question;
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        List<OptionEntity> oldOptions = optionRepository.findAllByQuestionId(questionId);
        optionRepository.deleteAll(oldOptions);
        questionRepository.deleteById(questionId);
        log.info("Question deleted id={}", questionId);
    }
}
