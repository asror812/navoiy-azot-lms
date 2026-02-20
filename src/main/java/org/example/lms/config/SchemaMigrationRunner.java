package org.example.lms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // Convert simple_questions to a standalone table (no test_id dependency)
        jdbcTemplate.execute("ALTER TABLE simple_questions ADD COLUMN IF NOT EXISTS title VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE simple_questions ADD COLUMN IF NOT EXISTS profession VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE simple_questions ADD COLUMN IF NOT EXISTS active BOOLEAN");
        jdbcTemplate.execute("ALTER TABLE simple_questions ADD COLUMN IF NOT EXISTS created_by VARCHAR(255)");

        jdbcTemplate.execute("UPDATE simple_questions SET title = COALESCE(title, 'Question #' || id)");
        jdbcTemplate.execute("UPDATE simple_questions SET profession = COALESCE(profession, 'general')");
        jdbcTemplate.execute("UPDATE simple_questions SET active = COALESCE(active, true)");
        jdbcTemplate.execute("UPDATE simple_questions SET created_by = COALESCE(created_by, 'migration')");

        jdbcTemplate.execute("ALTER TABLE simple_questions ALTER COLUMN title SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE simple_questions ALTER COLUMN profession SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE simple_questions ALTER COLUMN active SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE simple_questions ALTER COLUMN created_by SET NOT NULL");

        jdbcTemplate.execute("ALTER TABLE simple_questions DROP COLUMN IF EXISTS test_id CASCADE");

        log.info("Schema migration completed for standalone simple_questions table");
    }
}
