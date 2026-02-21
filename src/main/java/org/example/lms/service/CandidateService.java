package org.example.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.dto.CandidateDtos;
import org.example.lms.dto.CandidateResponses;
import org.example.lms.entity.*;
import org.example.lms.repository.*;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String MSG_NO_QUESTIONS_FOR_PROFESSION = "No questions found for profession=%s";
    private static final String MSG_ATTEMPT_NOT_FOUND = "Attempt not found. attemptId=%d, candidateId=%d";
    private static final String MSG_ATTEMPT_ALREADY_FINISHED = "Attempt already finished. attemptId=";

    private final CandidateRepository candidateRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${exam.duration-minutes:60}")
    private int examDurationMinutes;

    @Value("${exam.max-attempts-per-candidate:0}")
    private int maxAttemptsPerCandidate;

    public CandidateResponses.LoginResponse login(CandidateDtos.LoginRequest req) {
        CandidateEntity candidate = candidateRepository.findByLoginIgnoreCase(req.login().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        MSG_INVALID_LOGIN_OR_PASSWORD + ". login=" + req.login().trim()));

        validateCandidateActive(candidate);

        if (!isPasswordValid(req.password(), candidate.getPasswordHash())) {
            throw new IllegalArgumentException(MSG_INVALID_LOGIN_OR_PASSWORD + ". login=" + req.login().trim());
        }

        log.info("Candidate login success id={} login={}", candidate.getId(), candidate.getLogin());
        return toLoginResponse(candidate);
    }

    public CandidateResponses.LoginResponse passportLogin(CandidateDtos.PassportLoginRequest req) {
        String passport = req.passport().trim();

        CandidateEntity candidate = candidateRepository.findByLoginIgnoreCase(passport)
                .orElseThrow(() -> new IllegalArgumentException(MSG_INVALID_LOGIN_OR_PASSWORD + ". login=" + passport));

        validateCandidateActive(candidate);

        if (!candidate.getFullName().trim().equalsIgnoreCase(req.fullName().trim())) {
            throw new IllegalArgumentException("Full name and passport do not match candidate record");
        }

        if (!isPasswordValid(passport, candidate.getPasswordHash())) {
            throw new IllegalArgumentException(MSG_INVALID_LOGIN_OR_PASSWORD + ". login=" + passport);
        }

        log.info("Candidate passport login success id={} login={}", candidate.getId(), candidate.getLogin());
        return toLoginResponse(candidate);
    }

    public List<CandidateResponses.ProfessionTestResponse> listRandomTests(Long candidateId) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + candidateId));

        validateCandidateActive(candidate);

        return questionRepository
                .findAllByActiveTrueAndProfessionIgnoreCaseOrderByIdDesc(candidate.getProfession())
                .stream()
                .map(q -> new CandidateResponses.ProfessionTestResponse(q.getId(), q.getTitle(), q.getProfession()))
                .toList();
    }

    @Transactional
    public CandidateResponses.StartResponse startTest(CandidateDtos.StartTestRequest req) {
        CandidateEntity candidate = candidateRepository.findById(req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + req.candidateId()));

        validateCandidateActive(candidate);

        Optional<AttemptEntity> unfinished = attemptRepository
                .findTopByCandidateIdAndFinishedFalseOrderByStartedAtDesc(candidate.getId());

        if (unfinished.isPresent()) {
            return buildStartResponse(unfinished.get());
        }

        if (maxAttemptsPerCandidate > 0
                && attemptRepository.countByCandidateId(candidate.getId()) >= maxAttemptsPerCandidate) {
            throw new IllegalArgumentException("Attempt limit exceeded for candidateId=" + candidate.getId()
                    + ". maxAttempts=" + maxAttemptsPerCandidate);
        }

        List<QuestionEntity> allQuestions = questionRepository
                .findAllByActiveTrueAndProfessionIgnoreCaseOrderByIdDesc(candidate.getProfession());

        if (allQuestions.isEmpty()) {
            throw new IllegalArgumentException(MSG_NO_QUESTIONS_FOR_PROFESSION.formatted(candidate.getProfession()));
        }

        int selectedCount = allQuestions.size();
        List<QuestionEntity> selected = allQuestions;

        AttemptEntity attempt = attemptRepository.save(AttemptEntity.builder()
                .candidate(candidate)
                .profession(candidate.getProfession())
                .finished(false)
                .totalQuestions(selectedCount)
                .durationMinutes(examDurationMinutes)
                .startedAt(LocalDateTime.now())
                .build());

        List<AttemptQuestionEntity> attemptQuestions = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            QuestionEntity question = selected.get(i);
            attemptQuestions.add(AttemptQuestionEntity.builder()
                    .attempt(attempt)
                    .question(question)
                    .displayOrder(i + 1)
                    .build());
        }
        attemptQuestionRepository.saveAll(attemptQuestions);

        log.info("Attempt started id={} candidateId={} profession={} questionCount={}",
                attempt.getId(), candidate.getId(), candidate.getProfession(), selectedCount);

        return buildStartResponse(attempt);
    }

    @Transactional
    public CandidateResponses.ProgressResponse saveProgress(Long attemptId, CandidateDtos.SaveProgressRequest req) {
        AttemptEntity attempt = attemptRepository.findByIdAndCandidateId(attemptId, req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException(
                        MSG_ATTEMPT_NOT_FOUND.formatted(attemptId, req.candidateId())));

        if (Boolean.TRUE.equals(attempt.getFinished())) {
            throw new IllegalArgumentException(MSG_ATTEMPT_ALREADY_FINISHED + attemptId);
        }

        Map<Long, QuestionEntity> selectedQuestionMap = attemptQuestionRepository
                .findAllByAttemptId(attemptId)
                .stream()
                .map(AttemptQuestionEntity::getQuestion)
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));

        Map<Long, OptionEntity> optionsById = buildOptionsMap(selectedQuestionMap.keySet());

        Map<Long, AttemptAnswerEntity> answerMap = attemptAnswerRepository.findAllByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));

        applyAnswers(req.answers(), attempt, selectedQuestionMap, optionsById, answerMap);
        attemptAnswerRepository.saveAll(answerMap.values());

        return buildProgressResponse(attempt);
    }

    public CandidateResponses.ProgressResponse getProgress(Long attemptId, Long candidateId) {
        AttemptEntity attempt = attemptRepository.findByIdAndCandidateId(attemptId, candidateId)
                .orElseThrow(
                        () -> new IllegalArgumentException(MSG_ATTEMPT_NOT_FOUND.formatted(attemptId, candidateId)));

        return buildProgressResponse(attempt);
    }

    @Transactional
    public CandidateResponses.SubmitResponse submitAttempt(Long attemptId, CandidateDtos.SubmitAttemptRequest req) {
        AttemptEntity attempt = attemptRepository.findByIdAndCandidateId(attemptId, req.candidateId())
                .orElseThrow(() -> new IllegalArgumentException(
                        MSG_ATTEMPT_NOT_FOUND.formatted(attemptId, req.candidateId())));

        if (Boolean.TRUE.equals(attempt.getFinished())) {
            throw new IllegalArgumentException(MSG_ATTEMPT_ALREADY_FINISHED + attemptId);
        }

        Map<Long, QuestionEntity> selectedQuestionMap = attemptQuestionRepository
                .findAllByAttemptId(attemptId)
                .stream()
                .map(AttemptQuestionEntity::getQuestion)
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));

        Map<Long, OptionEntity> optionsById = buildOptionsMap(selectedQuestionMap.keySet());

        Map<Long, AttemptAnswerEntity> answerMap = attemptAnswerRepository.findAllByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));

        applyAnswers(req.answers(), attempt, selectedQuestionMap, optionsById, answerMap);

        int correct = 0;
        for (Long questionId : selectedQuestionMap.keySet()) {
            AttemptAnswerEntity answer = answerMap.get(questionId);
            if (answer != null && Boolean.TRUE.equals(answer.getCorrect())) {
                correct++;
            }
        }

        attemptAnswerRepository.saveAll(answerMap.values());

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
                attempt.getScore(),
                attempt.getStartedAt(),
                attempt.getFinishedAt());
    }

    private CandidateResponses.ProgressResponse buildProgressResponse(AttemptEntity attempt) {
        List<CandidateResponses.SavedAnswerPayload> savedAnswers = attemptAnswerRepository
                .findAllByAttemptId(attempt.getId()).stream()
                .map(answer -> new CandidateResponses.SavedAnswerPayload(
                        answer.getQuestion().getId(),
                        answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId()))
                .toList();

        int answeredCount = (int) savedAnswers.stream().filter(answer -> answer.selectedOptionId() != null).count();

        return new CandidateResponses.ProgressResponse(
                attempt.getId(),
                answeredCount,
                attempt.getTotalQuestions(),
                attempt.getStartedAt(),
                resolveAttemptEnd(attempt),
                savedAnswers);
    }

    private CandidateResponses.StartResponse buildStartResponse(AttemptEntity attempt) {
        List<AttemptQuestionEntity> selectedQuestions = attemptQuestionRepository
                .findAllByAttemptIdOrderByDisplayOrderAsc(attempt.getId());
        Map<Long, List<OptionEntity>> optionsByQuestionId = mapOptionsByQuestionId(
                selectedQuestions.stream().map(aq -> aq.getQuestion().getId()).toList());

        List<CandidateResponses.QuestionPayload> questionPayloads = selectedQuestions.stream().map(attemptQuestion -> {
            QuestionEntity question = attemptQuestion.getQuestion();
            List<OptionEntity> options = new ArrayList<>(
                    optionsByQuestionId.getOrDefault(question.getId(), Collections.emptyList()));
            Collections.shuffle(options);
            List<CandidateResponses.OptionPayload> optionPayloads = options.stream()
                    .map(o -> new CandidateResponses.OptionPayload(o.getId(), o.getText()))
                    .toList();
            return new CandidateResponses.QuestionPayload(question.getId(), question.getText(), optionPayloads);
        }).toList();

        List<CandidateResponses.SavedAnswerPayload> savedAnswers = attemptAnswerRepository
                .findAllByAttemptId(attempt.getId()).stream()
                .map(answer -> new CandidateResponses.SavedAnswerPayload(
                        answer.getQuestion().getId(),
                        answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId()))
                .toList();

        int attemptNumber = resolveAttemptNumber(attempt);

        return new CandidateResponses.StartResponse(
                attempt.getId(),
                attemptNumber,
                attempt.getProfession(),
                attempt.getTotalQuestions(),
                resolveDuration(attempt),
                attempt.getStartedAt(),
                resolveAttemptEnd(attempt),
                questionPayloads,
                savedAnswers);
    }

    private int resolveAttemptNumber(AttemptEntity attempt) {
        List<AttemptEntity> attempts = attemptRepository
                .findAllByCandidateIdOrderByStartedAtAsc(attempt.getCandidate().getId());
        for (int i = 0; i < attempts.size(); i++) {
            if (Objects.equals(attempts.get(i).getId(), attempt.getId())) {
                return i + 1;
            }
        }
        return 1;
    }

    private int resolveDuration(AttemptEntity attempt) {
        Integer stored = attempt.getDurationMinutes();
        return stored == null || stored <= 0 ? examDurationMinutes : stored;
    }

    private LocalDateTime resolveAttemptEnd(AttemptEntity attempt) {
        return attempt.getStartedAt().plusMinutes(resolveDuration(attempt));
    }

    private void applyAnswers(
            List<CandidateDtos.AnswerRequest> requestAnswers,
            AttemptEntity attempt,
            Map<Long, QuestionEntity> selectedQuestionMap,
            Map<Long, OptionEntity> optionsById,
            Map<Long, AttemptAnswerEntity> answerMap) {
        for (CandidateDtos.AnswerRequest answer : requestAnswers) {
            QuestionEntity question = selectedQuestionMap.get(answer.questionId());
            if (question == null) {
                continue;
            }

            OptionEntity selectedOption = answer.selectedOptionId() == null ? null
                    : optionsById.get(answer.selectedOptionId());
            if (selectedOption != null && !selectedOption.getQuestion().getId().equals(question.getId())) {
                selectedOption = null;
            }

            boolean isCorrect = selectedOption != null && Boolean.TRUE.equals(selectedOption.getCorrect());

            AttemptAnswerEntity current = answerMap.get(question.getId());
            if (current == null) {
                current = AttemptAnswerEntity.builder()
                        .attempt(attempt)
                        .question(question)
                        .selectedOption(selectedOption)
                        .correct(isCorrect)
                        .build();
            } else {
                current.setSelectedOption(selectedOption);
                current.setCorrect(isCorrect);
            }

            answerMap.put(question.getId(), current);
        }
    }

    private Map<Long, OptionEntity> buildOptionsMap(Collection<Long> questionIds) {
        Map<Long, OptionEntity> optionsById = new HashMap<>();
        if (questionIds == null || questionIds.isEmpty()) {
            return optionsById;
        }
        for (OptionEntity option : optionRepository.findAllByQuestionIdIn(questionIds)) {
            optionsById.put(option.getId(), option);
        }
        return optionsById;
    }

    private Map<Long, List<OptionEntity>> mapOptionsByQuestionId(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findAllByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
    }

    private void validateCandidateActive(CandidateEntity candidate) {
        if (!Boolean.TRUE.equals(candidate.getActive())) {
            throw new IllegalArgumentException(MSG_CANDIDATE_INACTIVE + candidate.getId());
        }
    }

    private boolean isPasswordValid(String raw, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        try {
            return passwordEncoder.matches(raw, stored);
        } catch (IllegalArgumentException ex) {
            return raw.equals(stored);
        }
    }

    private CandidateResponses.LoginResponse toLoginResponse(CandidateEntity candidate) {
        return new CandidateResponses.LoginResponse(
                candidate.getId(),
                candidate.getFullName(),
                candidate.getProfession(),
                candidate.getLogin());
    }
}
