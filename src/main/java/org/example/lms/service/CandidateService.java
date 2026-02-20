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

    private final CandidateRepository candidateRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Random random = new Random();

    public CandidateResponses.LoginResponse login(CandidateDtos.LoginRequest req) {
        CandidateEntity candidate = candidateRepository.findByLoginIgnoreCase(req.login().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid login or password"));

        if (!Boolean.TRUE.equals(candidate.getActive())) {
            throw new IllegalArgumentException("Candidate is inactive");
        }

        if (!passwordEncoder.matches(req.password(), candidate.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid login or password");
        }

        log.info("Candidate login success id={} login={}", candidate.getId(), candidate.getLogin());
        return new CandidateResponses.LoginResponse(candidate.getId(), candidate.getFullName(), candidate.getLogin());
    }

    public List<CandidateResponses.AssignedTestResponse> listAssignedTests(Long candidateId) {
        return testAssignmentRepository.findAllByCandidateIdAndActiveTrue(candidateId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getTest().getActive()))
                .map(a -> new CandidateResponses.AssignedTestResponse(a.getTest().getId(), a.getTest().getTitle()))
                .toList();
    }

    @Transactional
    public CandidateResponses.StartResponse startTest(Long testId, CandidateDtos.StartTestRequest req) {
        CandidateEntity candidate = candidateRepository.findById(req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        if (!Boolean.TRUE.equals(candidate.getActive())) {
            throw new IllegalArgumentException("Candidate is inactive");
        }

        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        if (!Boolean.TRUE.equals(test.getActive())) {
            throw new IllegalArgumentException("Test is inactive");
        }

        testAssignmentRepository.findByCandidateIdAndTestIdAndActiveTrue(candidate.getId(), test.getId())
                .orElseThrow(() -> new IllegalArgumentException("This test is not assigned by HR"));

        if (attemptRepository.existsByCandidateIdAndTestId(candidate.getId(), test.getId())) {
            throw new IllegalArgumentException("No retakes allowed. Attempt already exists");
        }

        List<QuestionEntity> allQuestions = questionRepository.findAllByTestId(testId);
        if (allQuestions.isEmpty()) {
            throw new IllegalArgumentException("No questions configured for this test");
        }

        Collections.shuffle(allQuestions);

        int boundedMin = Math.min(test.getMinQuestionsPerAttempt(), allQuestions.size());
        int boundedMax = Math.min(test.getMaxQuestionsPerAttempt(), allQuestions.size());
        int count = boundedMin;
        if (boundedMax > boundedMin) {
            count = boundedMin + random.nextInt(boundedMax - boundedMin + 1);
        }

        List<QuestionEntity> selected = allQuestions.subList(0, count);

        AttemptEntity attempt = attemptRepository.save(AttemptEntity.builder()
                .candidate(candidate)
                .test(test)
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

        log.info("Attempt started id={} candidateId={} testId={} questionCount={}",
                attempt.getId(), candidate.getId(), test.getId(), count);
        return new CandidateResponses.StartResponse(attempt.getId(), test.getId(), test.getTitle(), count,
                questionPayloads);
    }

    @Transactional
    public CandidateResponses.SubmitResponse submitAttempt(Long attemptId, CandidateDtos.SubmitAttemptRequest req) {
        AttemptEntity attempt = attemptRepository.findByIdAndCandidateId(attemptId, req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));

        if (Boolean.TRUE.equals(attempt.getFinished())) {
            throw new IllegalArgumentException("Attempt already finished");
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
