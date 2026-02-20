package org.example.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.dto.CandidateDtos;
import org.example.lms.dto.CandidateResponses;
import org.example.lms.entity.*;
import org.example.lms.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateService {
    private static final String MSG_INVALID_LOGIN_OR_PASSWORD = "Invalid login or password";
    private static final String MSG_CANDIDATE_INACTIVE = "Candidate is inactive. candidateId=";
    private static final String MSG_CANDIDATE_NOT_FOUND_BY_ID = "Candidate not found. candidateId=";
    private static final String MSG_NO_RETAKES = "No retakes allowed. Attempt already exists for candidateId=";
    private static final String MSG_NO_TESTS_FOR_PROFESSION = "No active tests found for profession=";
    private static final String MSG_NO_QUESTIONS_FOR_PROFESSION = "No questions configured for profession=";
    private static final String MSG_ATTEMPT_NOT_FOUND = "Attempt not found. attemptId=%d, candidateId=%d";
    private static final String MSG_ATTEMPT_ALREADY_FINISHED = "Attempt already finished. attemptId=";

    private final CandidateRepository candidateRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Random random = new Random();

    public CandidateResponses.LoginResponse login(CandidateDtos.LoginRequest req) {
        CandidateEntity candidate = candidateRepository.findByLoginIgnoreCase(req.login().trim())
                .orElseThrow(() -> new IllegalArgumentException(MSG_INVALID_LOGIN_OR_PASSWORD + ". login=" + req.login().trim()));

        if (!Boolean.TRUE.equals(candidate.getActive())) {
            throw new IllegalArgumentException(MSG_CANDIDATE_INACTIVE + candidate.getId());
        }

        if (!passwordEncoder.matches(req.password(), candidate.getPasswordHash())) {
            throw new IllegalArgumentException(MSG_INVALID_LOGIN_OR_PASSWORD + ". login=" + req.login().trim());
        }

        log.info("Candidate login success id={} login={}", candidate.getId(), candidate.getLogin());
        return new CandidateResponses.LoginResponse(
                candidate.getId(),
                candidate.getFullName(),
                candidate.getProfession(),
                candidate.getLogin()
        );
    }

    public List<CandidateResponses.ProfessionTestResponse> listAssignedTests(Long candidateId) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + candidateId));

        return testRepository.findAllByActiveTrueAndProfessionIgnoreCaseOrderByIdDesc(candidate.getProfession()).stream()
                .map(t -> new CandidateResponses.ProfessionTestResponse(t.getId(), t.getTitle(), t.getProfession()))
                .toList();
    }

    @Transactional
    public CandidateResponses.StartResponse startTest(CandidateDtos.StartTestRequest req) {
        CandidateEntity candidate = candidateRepository.findById(req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + req.candidateId()));

        if (!Boolean.TRUE.equals(candidate.getActive())) {
            throw new IllegalArgumentException(MSG_CANDIDATE_INACTIVE + candidate.getId());
        }

        if (attemptRepository.existsByCandidateId(candidate.getId())) {
            throw new IllegalArgumentException(MSG_NO_RETAKES + candidate.getId());
        }

        List<TestEntity> professionTests = testRepository
                .findAllByActiveTrueAndProfessionIgnoreCaseOrderByIdDesc(candidate.getProfession());
        if (professionTests.isEmpty()) {
            throw new IllegalArgumentException(MSG_NO_TESTS_FOR_PROFESSION + candidate.getProfession());
        }

        List<Long> testIds = professionTests.stream().map(TestEntity::getId).toList();
        List<QuestionEntity> allQuestions = questionRepository.findAllByTestIdIn(testIds);
        if (allQuestions.isEmpty()) {
            throw new IllegalArgumentException(MSG_NO_QUESTIONS_FOR_PROFESSION + candidate.getProfession());
        }

        Collections.shuffle(allQuestions);

        int count = Math.min(40, allQuestions.size());

        List<QuestionEntity> selected = allQuestions.subList(0, count);

        AttemptEntity attempt = attemptRepository.save(AttemptEntity.builder()
                .candidate(candidate)
                .profession(candidate.getProfession())
                .finished(false)
                .totalQuestions(count)
                .startedAt(LocalDateTime.now())
                .build());

        attemptQuestionRepository.saveAll(selected.stream()
                .map(q -> AttemptQuestionEntity.builder().attempt(attempt).question(q).build())
                .toList());

        List<CandidateResponses.QuestionPayload> questionPayloads = selected.stream().map(q -> {
            List<CandidateResponses.OptionPayload> options = optionRepository.findAllByQuestionId(q.getId()).stream()
                    .map(o -> new CandidateResponses.OptionPayload(o.getId(), o.getText()))
                    .toList();
            return new CandidateResponses.QuestionPayload(q.getId(), q.getText(), options);
        }).toList();

        log.info("Attempt started id={} candidateId={} profession={} questionCount={}",
                attempt.getId(), candidate.getId(), candidate.getProfession(), count);
        return new CandidateResponses.StartResponse(attempt.getId(), candidate.getProfession(), count,
                questionPayloads);
    }

    @Transactional
    public CandidateResponses.SubmitResponse submitAttempt(Long attemptId, CandidateDtos.SubmitAttemptRequest req) {
        AttemptEntity attempt = attemptRepository.findByIdAndCandidateId(attemptId, req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException(MSG_ATTEMPT_NOT_FOUND.formatted(attemptId, req.candidateId())));

        if (Boolean.TRUE.equals(attempt.getFinished())) {
            throw new IllegalArgumentException(MSG_ATTEMPT_ALREADY_FINISHED + attemptId);
        }

        List<AttemptQuestionEntity> selectedQuestions = attemptQuestionRepository.findAllByAttemptId(attemptId);
        Map<Long, QuestionEntity> selectedQuestionMap = selectedQuestions.stream()
                .map(AttemptQuestionEntity::getQuestion)
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));

        Map<Long, OptionEntity> optionsById = new HashMap<>();
        for (QuestionEntity question : selectedQuestionMap.values()) {
            for (OptionEntity option : optionRepository.findAllByQuestionId(question.getId())) {
                optionsById.put(option.getId(), option);
            }
        }

        int correct = 0;
        List<AttemptAnswerEntity> savedAnswers = new ArrayList<>();

        for (CandidateDtos.AnswerRequest answer : req.answers()) {
            QuestionEntity question = selectedQuestionMap.get(answer.questionId());
            if (question == null) {
                continue;
            }

            OptionEntity selectedOption = answer.selectedOptionId() == null ? null
                    : optionsById.get(answer.selectedOptionId());
            boolean isCorrect = selectedOption != null
                    && selectedOption.getQuestion().getId().equals(question.getId())
                    && Boolean.TRUE.equals(selectedOption.getCorrect());

            if (isCorrect) {
                correct++;
            }

            savedAnswers.add(AttemptAnswerEntity.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(selectedOption)
                    .correct(isCorrect)
                    .build());
        }

        attemptAnswerRepository.saveAll(savedAnswers);

        double score = attempt.getTotalQuestions() == 0
                ? 0.0
                : (correct * 100.0) / attempt.getTotalQuestions();

        attempt.setCorrectAnswers(correct);
        attempt.setScore(Math.round(score * 100.0) / 100.0);
        attempt.setFinished(true);
        attempt.setFinishedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        log.info("Attempt submitted id={} candidateId={} score={}",
                attempt.getId(), req.candidateId(), attempt.getScore());
        return new CandidateResponses.SubmitResponse(
                attempt.getId(),
                correct,
                attempt.getTotalQuestions(),
                attempt.getScore());
    }
}
