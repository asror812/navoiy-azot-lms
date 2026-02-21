package org.example.lms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.entity.CandidateEntity;
import org.example.lms.entity.OptionEntity;
import org.example.lms.entity.QuestionEntity;
import org.example.lms.entity.JobEntity;
import org.example.lms.repository.CandidateRepository;
import org.example.lms.repository.JobRepository;
import org.example.lms.repository.OptionRepository;
import org.example.lms.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private static final List<SeedTest> SEED_TESTS = List.of(
            new SeedTest(
                    "Electrical safety test 1",
                    "electrician",
                    "What is the safe voltage threshold in wet conditions for hand tools?",
                    List.of(
                            new SeedOption("12V", false),
                            new SeedOption("36V", true),
                            new SeedOption("220V", false)
                    )
            ),
            new SeedTest(
                    "Electrical safety test 2",
                    "electrician",
                    "Before repairing a panel, what should be done first?",
                    List.of(
                            new SeedOption("Wear gloves and start work", false),
                            new SeedOption("Switch off and lock out the power", true),
                            new SeedOption("Only inform the supervisor", false)
                    )
            ),
            new SeedTest(
                    "Pump operator basics 1",
                    "operator",
                    "What is the first check before starting an industrial pump?",
                    List.of(
                            new SeedOption("Paint condition", false),
                            new SeedOption("Lubrication and suction line readiness", true),
                            new SeedOption("Room lighting", false)
                    )
            ),
            new SeedTest(
                    "Pump operator basics 2",
                    "operator",
                    "A sudden pressure drop usually indicates:",
                    List.of(
                            new SeedOption("Possible leakage or cavitation", true),
                            new SeedOption("Normal operation", false),
                            new SeedOption("Too much lubrication", false)
                    )
            ),
            new SeedTest(
                    "Mechanical maintenance 1",
                    "mechanic",
                    "What is a common sign of bearing wear?",
                    List.of(
                            new SeedOption("Unusual vibration and noise", true),
                            new SeedOption("Lower room temperature", false),
                            new SeedOption("Cleaner oil color", false)
                    )
            ),
            new SeedTest(
                    "Mechanical maintenance 2",
                    "mechanic",
                    "After replacing a shaft seal, what must be done?",
                    List.of(
                            new SeedOption("Run at full load immediately", false),
                            new SeedOption("Check alignment and perform test run", true),
                            new SeedOption("Ignore vibration for first day", false)
                    )
            ),
            new SeedTest(
                    "Process technology 1",
                    "technologist",
                    "What is the main goal of process control in production?",
                    List.of(
                            new SeedOption("Stable quality and safe operation", true),
                            new SeedOption("Increase noise level", false),
                            new SeedOption("Reduce documentation only", false)
                    )
            ),
            new SeedTest(
                    "Process technology 2",
                    "technologist",
                    "If process temperature exceeds limit, operator should:",
                    List.of(
                            new SeedOption("Ignore and continue", false),
                            new SeedOption("Apply emergency procedure and report", true),
                            new SeedOption("Shut all systems without protocol", false)
                    )
            ),
            new SeedTest(
                    "Industrial safety 1",
                    "operator",
                    "Which document defines workplace hazard controls?",
                    List.of(
                            new SeedOption("Shift menu", false),
                            new SeedOption("Risk assessment / safety instruction", true),
                            new SeedOption("Inventory list", false)
                    )
            ),
            new SeedTest(
                    "Industrial safety 2",
                    "mechanic",
                    "Personal protective equipment (PPE) is required when:",
                    List.of(
                            new SeedOption("Only during inspections", false),
                            new SeedOption("Whenever task risk requires it", true),
                            new SeedOption("Only in winter", false)
                    )
            ),
            new SeedTest(
                    "Electrical troubleshooting 1",
                    "electrician",
                    "A circuit breaker trips repeatedly. First action?",
                    List.of(
                            new SeedOption("Replace breaker with bigger one", false),
                            new SeedOption("Find and eliminate overload/short cause", true),
                            new SeedOption("Tape the breaker handle", false)
                    )
            ),
            new SeedTest(
                    "Production line control 1",
                    "technologist",
                    "Why is recording process deviations important?",
                    List.of(
                            new SeedOption("For root-cause analysis and prevention", true),
                            new SeedOption("Only for visual reports", false),
                            new SeedOption("It is optional always", false)
                    )
            )
    );

    private static final List<SeedCandidate> SEED_CANDIDATES = List.of(
            new SeedCandidate("Ali Valiyev", "electrician", "AA1234567", "AA1234567"),
            new SeedCandidate("Malika Karimova", "operator", "AB7654321", "AB7654321")
    );

    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Set<String> existingJobs = new HashSet<>(
                jobRepository.findAll().stream()
                        .map(job -> job.getName().trim().toLowerCase())
                        .toList()
        );

        for (SeedTest seed : SEED_TESTS) {
            String professionKey = seed.profession().trim().toLowerCase();
            if (!existingJobs.contains(professionKey)) {
                jobRepository.save(JobEntity.builder()
                        .name(seed.profession().trim())
                        .description("Auto-created by seed data")
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());
                existingJobs.add(professionKey);
            }
        }

        int createdCandidates = 0;
        for (SeedCandidate seed : SEED_CANDIDATES) {
            String login = seed.login().trim();
            if (candidateRepository.existsByLoginIgnoreCase(login)) {
                continue;
            }

            String profession = seed.profession().trim();
            String professionKey = profession.toLowerCase();
            if (!existingJobs.contains(professionKey)) {
                jobRepository.save(JobEntity.builder()
                        .name(profession)
                        .description("Auto-created by seed data")
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());
                existingJobs.add(professionKey);
            }

            candidateRepository.save(CandidateEntity.builder()
                    .fullName(seed.fullName().trim())
                    .profession(profession)
                    .login(login)
                    .passwordHash(encodePassword(seed.password()))
                    .active(true)
                    .build());
            createdCandidates++;
        }

        Set<String> existingTitles = new HashSet<>(
                questionRepository.findAll().stream()
                        .map(q -> q.getTitle().trim().toLowerCase())
                        .toList()
        );

        int createdCount = 0;
        for (SeedTest seed : SEED_TESTS) {
            String key = seed.title().trim().toLowerCase();
            if (existingTitles.contains(key)) {
                continue;
            }

            QuestionEntity question = questionRepository.save(QuestionEntity.builder()
                    .title(seed.title())
                    .profession(seed.profession())
                    .active(true)
                    .createdBy("seed-runner")
                    .text(seed.questionText())
                    .build());

            optionRepository.saveAll(
                    seed.options().stream()
                            .map(opt -> OptionEntity.builder()
                                    .question(question)
                                    .text(opt.text())
                                    .correct(opt.correct())
                                    .build())
                            .toList()
            );

            existingTitles.add(key);
            createdCount++;
        }

        log.info("Seeder completed: created {} candidates, {} realistic tests(questions). candidateTotal={}, questionTotal={}",
                createdCandidates, createdCount, candidateRepository.count(), questionRepository.count());
    }

    private record SeedTest(String title, String profession, String questionText, List<SeedOption> options) {
    }

    private record SeedOption(String text, boolean correct) {
    }

    private record SeedCandidate(String fullName, String profession, String login, String password) {
    }

    private String encodePassword(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Seed candidate password cannot be empty");
        }
        if (isBcryptHash(value)) {
            return value;
        }
        return passwordEncoder.encode(value);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
