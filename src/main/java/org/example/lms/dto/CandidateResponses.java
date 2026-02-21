package org.example.lms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CandidateResponses {
    public record LoginResponse(Long candidateId, String fullName, String profession, String login) {
    }

    public record ProfessionTestResponse(Long testId, String title, String profession) {
    }

    public record StartResponse(Long attemptId,
            Integer attemptNumber,
            String profession,
            Integer totalQuestions,
            Integer examDurationMinutes,
            LocalDateTime startedAt,
            LocalDateTime endsAt,
            List<QuestionPayload> questions,
            List<SavedAnswerPayload> savedAnswers) {
    }

    public record QuestionPayload(Long questionId, String text, List<OptionPayload> options) {
    }

    public record OptionPayload(Long optionId, String text) {
    }

    public record SavedAnswerPayload(Long questionId, Long selectedOptionId) {
    }

    public record ProgressResponse(Long attemptId,
            Integer answeredCount,
            Integer totalQuestions,
            LocalDateTime startedAt,
            LocalDateTime endsAt,
            List<SavedAnswerPayload> savedAnswers) {
    }

    public record SubmitResponse(Long attemptId,
            Integer correctAnswers,
            Integer totalQuestions,
            Double score,
            LocalDateTime startedAt,
            LocalDateTime finishedAt) {
    }
}
