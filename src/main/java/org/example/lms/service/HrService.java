package org.example.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.dto.HrDtos;
import org.example.lms.dto.HrResponses;
import org.example.lms.entity.*;
import org.example.lms.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HrService {
    private static final String MSG_EXACTLY_ONE_OPTION_CORRECT = "Exactly one option must be correct";
    private static final String MSG_TEST_NOT_FOUND_BY_ID = "Test(question) not found. testId=";
    private static final String MSG_CANDIDATE_NOT_FOUND_BY_ID = "Candidate not found. candidateId=";
    private static final String MSG_QUESTION_NOT_FOUND_BY_ID = "Question not found. questionId=";
    private static final String MSG_CANDIDATE_LOGIN_ALREADY_EXISTS = "Candidate login already exists. login=";
    private static final String MSG_JOB_NOT_FOUND_BY_ID = "Job not found. jobId=";
    private static final String MSG_JOB_NAME_ALREADY_EXISTS = "Job already exists. name=";

    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final CandidateRepository candidateRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    public List<HrResponses.TestResponse> listTests() {
        List<QuestionEntity> questions = questionRepository.findAllByActiveTrueOrderByIdDesc();

        Map<Long, List<OptionEntity>> optionsByQuestionId = mapOptionsByQuestionId(
                questions.stream().map(QuestionEntity::getId).toList()
        );

        return questions.stream()
                .map(question -> toTestResponse(question, optionsByQuestionId))
                .toList();
    }

    public List<HrResponses.CandidateResponse> listCandidates() {
        return candidateRepository.findAll().stream()
                .map(this::toCandidateResponse)
                .toList();
    }

    public List<HrResponses.JobResponse> listJobs() {
        return jobRepository.findAllByOrderByNameAsc().stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional
    public HrResponses.JobResponse createJob(HrDtos.CreateJobRequest req) {
        String name = req.name().trim();

        if (jobRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(MSG_JOB_NAME_ALREADY_EXISTS + name);
        }

        JobEntity job = jobRepository.save(JobEntity.builder()
                .name(name)
                .description(req.description() == null || req.description().isBlank() ? null : req.description().trim())
                .active(req.active() == null || req.active())
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Job created id={} name={}", job.getId(), job.getName());
        return toJobResponse(job);
    }

    @Transactional
    public HrResponses.JobResponse updateJob(Long jobId, HrDtos.UpdateJobRequest req) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOB_NOT_FOUND_BY_ID + jobId));

        String previousName = job.getName();

        if (req.name() != null && !req.name().isBlank()) {
            String nextName = req.name().trim();
            if (jobRepository.existsByNameIgnoreCaseAndIdNot(nextName, jobId)) {
                throw new IllegalArgumentException(MSG_JOB_NAME_ALREADY_EXISTS + nextName);
            }

            job.setName(nextName);
            renameProfessionEverywhere(previousName, nextName);
        }

        if (req.description() != null) {
            job.setDescription(req.description().isBlank() ? null : req.description().trim());
        }

        if (req.active() != null) {
            job.setActive(req.active());
        }

        JobEntity updated = jobRepository.save(job);
        log.info("Job updated id={} name={}", updated.getId(), updated.getName());

        return toJobResponse(updated);
    }

    @Transactional
    public void deleteJob(Long jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOB_NOT_FOUND_BY_ID + jobId));

        long questionCount = questionRepository.countByProfessionIgnoreCase(job.getName());
        long candidateCount = candidateRepository.countByProfessionIgnoreCase(job.getName());

        if (questionCount > 0 || candidateCount > 0) {
            throw new IllegalArgumentException("Cannot delete job with linked candidates/questions. candidateCount="
                    + candidateCount + ", questionCount=" + questionCount);
        }

        jobRepository.delete(job);

        log.info("Job deleted id={} name={}", job.getId(), job.getName());
    }

    @Transactional
    public HrResponses.TestResponse createTest(HrDtos.CreateTestRequest req, String hrUsername) {
        long correctCount = req.options().stream().filter(HrDtos.OptionRequest::correct).count();
        if (correctCount != 1) {
            throw new IllegalArgumentException(MSG_EXACTLY_ONE_OPTION_CORRECT + ". currentCorrectCount=" + correctCount);
        }

        String profession = req.profession().trim();
        ensureJobExists(profession);

        QuestionEntity question = questionRepository.save(QuestionEntity.builder()
                .title(req.title().trim())
                .profession(profession)
                .active(req.active() == null || req.active())
                .createdBy(hrUsername)
                .text(req.questionText().trim())
                .build());

        optionRepository.saveAll(req.options().stream()
                .map(o -> OptionEntity.builder()
                        .question(question)
                        .text(o.text().trim())
                        .correct(o.correct())
                        .build())
                .toList());

        log.info("HR {} created test(question) id={} title={}", hrUsername, question.getId(), question.getTitle());

        return toTestResponse(question);
    }

    @Transactional
    public HrResponses.TestResponse updateTest(Long id, HrDtos.UpdateTestRequest req) {
        QuestionEntity question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_TEST_NOT_FOUND_BY_ID + id));

        String previousProfession = question.getProfession();

        if (req.title() != null && !req.title().isBlank()) {
            question.setTitle(req.title().trim());
        }
        if (req.profession() != null && !req.profession().isBlank()) {
            String profession = req.profession().trim();
            ensureJobExists(profession);
            question.setProfession(profession);
            if (!previousProfession.equalsIgnoreCase(profession)) {
                // Profession rename for questions is local to this question via update.
            }
        }
        if (req.active() != null) {
            question.setActive(req.active());
        }
        if (req.questionText() != null && !req.questionText().isBlank()) {
            question.setText(req.questionText().trim());
        }

        QuestionEntity updated = questionRepository.save(question);
        log.info("Test(question) updated id={} title={}", updated.getId(), updated.getTitle());
        return toTestResponse(updated);
    }

    @Transactional
    public void deleteTest(Long id) {
        if (attemptQuestionRepository.existsByQuestionId(id)) {
            throw new IllegalArgumentException("Cannot delete question used in attempts. questionId=" + id);
        }

        List<OptionEntity> oldOptions = optionRepository.findAllByQuestionId(id);
        optionRepository.deleteAll(oldOptions);
        questionRepository.deleteById(id);
        log.info("Test(question) deleted id={}", id);
    }

    @Transactional
    public HrResponses.CandidateResponse createCandidate(HrDtos.CreateCandidateRequest req) {
        String login = req.login().trim();

        if (candidateRepository.existsByLoginIgnoreCase(login)) {
            throw new IllegalArgumentException(MSG_CANDIDATE_LOGIN_ALREADY_EXISTS + login);
        }

        String profession = req.profession().trim();
        ensureJobExists(profession);

        CandidateEntity saved = candidateRepository.save(CandidateEntity.builder()
                .fullName(req.fullName().trim())
                .profession(profession)
                .login(login)
                .passwordHash(normalizePasswordForStorage(req.password()))
                .active(req.active() == null || req.active())
                .build());

        log.info("Candidate created id={} login={}", saved.getId(), saved.getLogin());

        return toCandidateResponse(saved);
    }

    @Transactional
    public HrResponses.CandidateResponse updateCandidate(Long candidateId, HrDtos.UpdateCandidateRequest req) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + candidateId));

        if (req.fullName() != null && !req.fullName().isBlank()) {
            candidate.setFullName(req.fullName().trim());
        }

        if (req.profession() != null && !req.profession().isBlank()) {
            String profession = req.profession().trim();
            ensureJobExists(profession);
            candidate.setProfession(profession);
        }

        if (req.password() != null && !req.password().isBlank()) {
            candidate.setPasswordHash(normalizePasswordForStorage(req.password()));
        }

        if (req.active() != null) {
            candidate.setActive(req.active());
        }

        CandidateEntity updated = candidateRepository.save(candidate);
        log.info("Candidate updated id={} login={}", updated.getId(), updated.getLogin());
        return toCandidateResponse(updated);
    }

    @Transactional
    public HrResponses.CandidateResponse updateCandidatePassport(Long candidateId, HrDtos.UpdateCandidatePassportRequest req) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CANDIDATE_NOT_FOUND_BY_ID + candidateId));

        String passport = req.passport().trim();
        if (candidateRepository.existsByLoginIgnoreCaseAndIdNot(passport, candidateId)) {
            throw new IllegalArgumentException(MSG_CANDIDATE_LOGIN_ALREADY_EXISTS + passport);
        }

        candidate.setLogin(passport);
        candidate.setPasswordHash(passwordEncoder.encode(passport));

        CandidateEntity updated = candidateRepository.save(candidate);
        log.info("Candidate passport updated id={} login={}", updated.getId(), updated.getLogin());
        return toCandidateResponse(updated);
    }

    @Transactional
    public void deleteCandidate(Long candidateId) {
        if (attemptRepository.countByCandidateId(candidateId) > 0) {
            throw new IllegalArgumentException("Cannot delete candidate with attempts. candidateId=" + candidateId);
        }

        candidateRepository.deleteById(candidateId);
        log.info("Candidate deleted id={}", candidateId);
    }

    @Transactional
    public HrResponses.TestResponse updateQuestion(Long questionId, HrDtos.UpdateQuestionRequest req) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_QUESTION_NOT_FOUND_BY_ID + questionId));

        if (req.text() != null && !req.text().isBlank()) {
            question.setText(req.text().trim());
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
                            .text(o.text().trim())
                            .correct(o.correct())
                            .build())
                    .toList());
        }

        log.info("Question updated id={}", questionId);

        return toTestResponse(question);
    }

    public List<HrResponses.ResultResponse> listResults(
            String job,
            LocalDate fromDate,
            LocalDate toDate,
            String candidateQuery,
            Double minScore,
            Double maxScore,
            String status
    ) {
        String jobFilter = normalize(job);
        String queryFilter = normalize(candidateQuery);
        String statusFilter = normalize(status);
        LocalDateTime from = toStartOfDay(fromDate);
        LocalDateTime to = toEndOfDay(toDate);

        List<AttemptEntity> attempts = attemptRepository.findAllWithCandidateOrderByStartedAtDesc();
        Map<Long, List<AttemptEntity>> attemptsByCandidate = groupAttemptsByCandidate(attempts);
        Map<Long, Integer> attemptNumbers = buildAttemptNumbers(attemptsByCandidate);

        List<HrResponses.ResultResponse> rows = new ArrayList<>(buildAttemptRows(attempts, attemptNumbers));
        appendNotStartedRowsIfNeeded(rows, statusFilter, attemptsByCandidate.keySet());

        return rows.stream()
                .filter(row -> filterJob(row, jobFilter))
                .filter(row -> filterDate(row, from, to))
                .filter(row -> filterCandidate(row, queryFilter))
                .filter(row -> filterScore(row, minScore, maxScore))
                .filter(row -> filterStatus(row, statusFilter))
                .sorted(Comparator.comparing(this::startedAtOrMin).reversed())
                .toList();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
    }

    private Map<Long, List<AttemptEntity>> groupAttemptsByCandidate(List<AttemptEntity> attempts) {
        return attempts.stream().collect(Collectors.groupingBy(a -> a.getCandidate().getId()));
    }

    private Map<Long, Integer> buildAttemptNumbers(Map<Long, List<AttemptEntity>> attemptsByCandidate) {
        Map<Long, Integer> attemptNumbers = new HashMap<>();

        attemptsByCandidate.values().forEach(candidateAttempts -> {
            List<AttemptEntity> ordered = candidateAttempts.stream()
                    .sorted(Comparator.comparing(AttemptEntity::getStartedAt))
                    .toList();
            for (int i = 0; i < ordered.size(); i++) {
                attemptNumbers.put(ordered.get(i).getId(), i + 1);
            }
        });

        return attemptNumbers;
    }

    private List<HrResponses.ResultResponse> buildAttemptRows(
            List<AttemptEntity> attempts,
            Map<Long, Integer> attemptNumbers
    ) {
        return attempts.stream()
                .map(attempt -> toResultRow(attempt, attemptNumbers.getOrDefault(attempt.getId(), 1)))
                .toList();
    }

    private HrResponses.ResultResponse toResultRow(AttemptEntity attempt, Integer attemptNumber) {
        boolean finished = Boolean.TRUE.equals(attempt.getFinished());
        String rowStatus = finished ? "completed" : "in-progress";
        Long duration = calculateDurationSeconds(attempt.getStartedAt(), attempt.getFinishedAt(), finished);

        return new HrResponses.ResultResponse(
                attempt.getId(),
                attemptNumber,
                attempt.getCandidate().getId(),
                attempt.getCandidate().getFullName(),
                attempt.getCandidate().getLogin(),
                attempt.getProfession(),
                attempt.getCorrectAnswers(),
                attempt.getTotalQuestions(),
                attempt.getScore(),
                rowStatus,
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                duration
        );
    }

    private void appendNotStartedRowsIfNeeded(
            List<HrResponses.ResultResponse> rows,
            String statusFilter,
            Set<Long> candidateIdsWithAttempts
    ) {
        if (statusFilter != null && !"not-started".equals(statusFilter)) {
            return;
        }

        candidateRepository.findAll().stream()
                .filter(candidate -> !candidateIdsWithAttempts.contains(candidate.getId()))
                .map(this::toNotStartedResultRow)
                .forEach(rows::add);
    }

    private HrResponses.ResultResponse toNotStartedResultRow(CandidateEntity candidate) {
        return new HrResponses.ResultResponse(
                null,
                null,
                candidate.getId(),
                candidate.getFullName(),
                candidate.getLogin(),
                candidate.getProfession(),
                null,
                null,
                null,
                "not-started",
                null,
                null,
                null
        );
    }

    private LocalDateTime startedAtOrMin(HrResponses.ResultResponse row) {
        return row.startedAt() == null ? LocalDateTime.MIN : row.startedAt();
    }

    private boolean filterJob(HrResponses.ResultResponse row, String jobFilter) {
        return jobFilter == null || jobFilter.equals(normalize(row.profession()));
    }

    private boolean filterDate(HrResponses.ResultResponse row, LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) return true;
        if (row.startedAt() == null) return false;
        boolean afterFrom = from == null || !row.startedAt().isBefore(from);
        boolean beforeTo = to == null || !row.startedAt().isAfter(to);

        return afterFrom && beforeTo;
    }

    private boolean filterCandidate(HrResponses.ResultResponse row, String queryFilter) {
        if (queryFilter == null) return true;
        String fullName = normalize(row.candidateName());
        String passport = normalize(row.passport());
        return (fullName != null && fullName.contains(queryFilter)) || (passport != null && passport.contains(queryFilter));
    }

    private boolean filterScore(HrResponses.ResultResponse row, Double minScore, Double maxScore) {
        if (minScore == null && maxScore == null) return true;
        if (!"completed".equals(row.status()) || row.score() == null) return false;
        boolean minOk = minScore == null || row.score() >= minScore;
        boolean maxOk = maxScore == null || row.score() <= maxScore;
        return minOk && maxOk;
    }

    private boolean filterStatus(HrResponses.ResultResponse row, String statusFilter) {
        return statusFilter == null || row.status().equalsIgnoreCase(statusFilter);
    }

    private Long calculateDurationSeconds(LocalDateTime startedAt, LocalDateTime finishedAt, boolean finished) {
        if (startedAt == null) return null;
        LocalDateTime end = finished ? finishedAt : LocalDateTime.now();
        if (end == null) return null;

        return Math.max(0, Duration.between(startedAt, end).getSeconds());
    }

    private void renameProfessionEverywhere(String fromName, String toName) {
        if (fromName.equalsIgnoreCase(toName)) {
            return;
        }

        List<QuestionEntity> questions = questionRepository.findAll().stream()
                .filter(q -> q.getProfession() != null && q.getProfession().equalsIgnoreCase(fromName))
                .toList();

        for (QuestionEntity question : questions) {
            question.setProfession(toName);
        }

        questionRepository.saveAll(questions);

        List<CandidateEntity> candidates = candidateRepository.findAll().stream()
                .filter(c -> c.getProfession() != null && c.getProfession().equalsIgnoreCase(fromName))
                .toList();

        for (CandidateEntity candidate : candidates) {
            candidate.setProfession(toName);
        }

        candidateRepository.saveAll(candidates);

        List<AttemptEntity> attempts = attemptRepository.findAll().stream()
                .filter(a -> a.getProfession() != null && a.getProfession().equalsIgnoreCase(fromName))
                .toList();

        for (AttemptEntity attempt : attempts) {
            attempt.setProfession(toName);
        }

        attemptRepository.saveAll(attempts);
    }

    private HrResponses.TestResponse toTestResponse(QuestionEntity question) {
        List<HrResponses.OptionResponse> options = optionRepository.findAllByQuestionId(question.getId()).stream()
                .map(o -> new HrResponses.OptionResponse(o.getId(), o.getText(), o.getCorrect()))
                .toList();

        return new HrResponses.TestResponse(
                question.getId(),
                question.getTitle(),
                question.getProfession(),
                question.getText(),
                question.getActive(),
                question.getCreatedBy(),
                options
        );
    }

    private HrResponses.TestResponse toTestResponse(QuestionEntity question, Map<Long, List<OptionEntity>> optionsByQuestionId) {
        List<HrResponses.OptionResponse> options = optionsByQuestionId
                .getOrDefault(question.getId(), Collections.emptyList())
                .stream()
                .map(o -> new HrResponses.OptionResponse(o.getId(), o.getText(), o.getCorrect()))
                .toList();

        return new HrResponses.TestResponse(
                question.getId(),
                question.getTitle(),
                question.getProfession(),
                question.getText(),
                question.getActive(),
                question.getCreatedBy(),
                options
        );
    }

    private Map<Long, List<OptionEntity>> mapOptionsByQuestionId(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }

        return optionRepository.findAllByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
    }

    private HrResponses.CandidateResponse toCandidateResponse(CandidateEntity candidate) {
        return new HrResponses.CandidateResponse(
                candidate.getId(),
                candidate.getFullName(),
                candidate.getProfession(),
                candidate.getLogin(),
                candidate.getActive()
        );
    }

    private HrResponses.JobResponse toJobResponse(JobEntity job) {
        return new HrResponses.JobResponse(
                job.getId(),
                job.getName(),
                job.getDescription(),
                job.getActive(),
                job.getCreatedAt(),
                candidateRepository.countByProfessionIgnoreCase(job.getName()),
                questionRepository.countByProfessionIgnoreCase(job.getName())
        );
    }

    private void ensureJobExists(String profession) {
        String value = profession.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("profession is required");
        }

        if (jobRepository.findByNameIgnoreCase(value).isPresent()) {
            return;
        }

        jobRepository.save(JobEntity.builder()
                .name(value)
                .description(null)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String normalizePasswordForStorage(String password) {
        String raw = password == null ? "" : password.trim();

        if (raw.isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }

        if (isBcryptHash(raw)) {
            return raw;
        }

        return passwordEncoder.encode(raw);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private String normalize(String value) {
        if (value == null) return null;
        
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        
        return normalized.isEmpty() ? null : normalized;
    }
}
