package org.example.lms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class HrResponses {

    public record OptionResponse(Long optionId, String text, Boolean correct) {
    }

    public record TestResponse(
            Long testId,
            String title,
            String profession,
            String questionText,
            Boolean active,
            String createdBy,
            List<OptionResponse> options) {
    }

    public record CandidateResponse(
            Long candidateId,
            String fullName,
            String profession,
            String login,
            Boolean active) {
    }

    public record JobResponse(
            Long jobId,
            String name,
            String description,
            Boolean active,
            LocalDateTime createdAt,
            Long candidateCount,
            Long questionCount) {
    }

    public record ResultResponse(
            Long attemptId,
            Integer attemptNumber,
            Long candidateId,
            String candidateName,
            String passport,
            String profession,
            Integer correctAnswers,
            Integer totalQuestions,
            Double score,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Long durationSeconds) {
    }
}
