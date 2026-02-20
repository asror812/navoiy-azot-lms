package org.example.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.dto.HrDtos;
import org.example.lms.entity.CandidateEntity;
import org.example.lms.entity.OptionEntity;
import org.example.lms.entity.QuestionEntity;
import org.example.lms.repository.CandidateRepository;
import org.example.lms.repository.OptionRepository;
import org.example.lms.repository.QuestionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HrService {
    private static final String MSG_EXACTLY_ONE_OPTION_CORRECT = "Exactly one option must be correct";
    private static final String MSG_TEST_NOT_FOUND_BY_ID = "Test(question) not found. testId=";
    private static final String MSG_CANDIDATE_NOT_FOUND_BY_ID = "Candidate not found. candidateId=";
    private static final String MSG_QUESTION_NOT_FOUND_BY_ID = "Question not found. questionId=";
    private static final String MSG_CANDIDATE_LOGIN_ALREADY_EXISTS = "Candidate login already exists. login=";

    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    public List<QuestionEntity> listTests() {
        return questionRepository.findAllByActiveTrueOrderByIdDesc();
    }

    public List<CandidateEntity> listCandidates() {
        return candidateRepository.findAll();
    }

    @Transactional
    public QuestionEntity createTest(HrDtos.CreateTestRequest req, String hrUsername) {
        long correctCount = req.options().stream().filter(HrDtos.OptionRequest::correct).count();
        if (correctCount != 1) {
            throw new IllegalArgumentException(MSG_EXACTLY_ONE_OPTION_CORRECT + ". currentCorrectCount=" + correctCount);
        }

        QuestionEntity question = questionRepository.save(QuestionEntity.builder()
                .title(req.title().trim())
                .profession(req.profession().trim())
                .active(req.active() == null || req.active())
                .createdBy(hrUsername)
                .text(req.questionText().trim())
                .build());

        optionRepository.saveAll(req.options().stream()
                .map(o -> OptionEntity.builder()
                        .question(question)
                        .text(o.text())
                        .correct(o.correct())
                        .build())
                .toList());

        log.info("HR {} created test(question) id={} title={}", hrUsername, question.getId(), question.getTitle());
        return question;
    }

    public QuestionEntity updateTest(Long id, HrDtos.UpdateTestRequest req) {
        QuestionEntity question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_TEST_NOT_FOUND_BY_ID + id));

        if (req.title() != null && !req.title().isBlank()) {
            question.setTitle(req.title().trim());
        }
        if (req.profession() != null && !req.profession().isBlank()) {
            question.setProfession(req.profession().trim());
        }
        if (req.active() != null) {
            question.setActive(req.active());
        }
        if (req.questionText() != null && !req.questionText().isBlank()) {
            question.setText(req.questionText().trim());
        }

        QuestionEntity updated = questionRepository.save(question);
        log.info("Test(question) updated id={} title={}", updated.getId(), updated.getTitle());
        return updated;
    }

    public void deleteTest(Long id) {
        List<OptionEntity> oldOptions = optionRepository.findAllByQuestionId(id);
        optionRepository.deleteAll(oldOptions);
        questionRepository.deleteById(id);
        log.info("Test(question) deleted id={}", id);
    }

    public CandidateEntity createCandidate(HrDtos.CreateCandidateRequest req) {
        if (candidateRepository.existsByLoginIgnoreCase(req.login())) {
            throw new IllegalArgumentException(MSG_CANDIDATE_LOGIN_ALREADY_EXISTS + req.login().trim());
        }

        CandidateEntity saved = candidateRepository.save(CandidateEntity.builder()
                .fullName(req.fullName())
                .profession(req.profession().trim())
                .login(req.login().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .active(req.active() == null || req.active())
                .build());

        log.info("Candidate created id={} login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    public CandidateEntity updateCandidate(Long candidateId, HrDtos.UpdateCandidateRequest req) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + candidateId));

        if (req.fullName() != null && !req.fullName().isBlank()) {
            candidate.setFullName(req.fullName());
        }
        if (req.profession() != null && !req.profession().isBlank()) {
            candidate.setProfession(req.profession().trim());
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

    @Transactional
    public QuestionEntity updateQuestion(Long questionId, HrDtos.UpdateQuestionRequest req) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_QUESTION_NOT_FOUND_BY_ID + questionId));

        if (req.text() != null && !req.text().isBlank()) {
            question.setText(req.text());
        }
        questionRepository.save(question);

        if (req.options() != null && !req.options().isEmpty()) {
            long correctCount = req.options().stream().filter(HrDtos.OptionRequest::correct).count();
            if (correctCount != 1) {
                throw new IllegalArgumentException(
                        MSG_EXACTLY_ONE_OPTION_CORRECT + ". currentCorrectCount=" + correctCount);
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
