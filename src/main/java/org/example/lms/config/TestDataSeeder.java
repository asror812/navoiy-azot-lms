package org.example.lms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lms.entity.OptionEntity;
import org.example.lms.entity.QuestionEntity;
import org.example.lms.repository.OptionRepository;
import org.example.lms.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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

    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    @Override
    public void run(String... args) {
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

        log.info("Seeder completed: created {} realistic tests(questions). totalCount={}",
                createdCount, questionRepository.count());
    }

    private record SeedTest(String title, String profession, String questionText, List<SeedOption> options) {
    }

    private record SeedOption(String text, boolean correct) {
    }
}
