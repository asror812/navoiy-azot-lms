package org.example.lms.dto;

import java.util.List;

public class CandidateResponses {
    public record LoginResponse(Long candidateId, String fullName, String login) {
    }

    public record AssignedTestResponse(Long testId, String title) {
    }

    public record StartResponse(Long attemptId, Long testId, String testTitle, Integer totalQuestions,
            List<QuestionPayload> questions) {
    }

    public record QuestionPayload(Long questionId, String text, List<OptionPayload> options) {
    }

    public record OptionPayload(Long optionId, String text) {
    }

    public record SubmitResponse(Long attemptId, Integer correctAnswers, Integer totalQuestions, Double score) {
    }
}
