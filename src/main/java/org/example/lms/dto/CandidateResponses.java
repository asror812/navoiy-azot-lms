package org.example.lms.dto;

import java.util.List;

public class CandidateResponses {
    public record LoginResponse(Long candidateId, String fullName, String profession, String login) {
    }

    public record ProfessionTestResponse(Long testId, String title, String profession) {
    }

    public record StartResponse(Long attemptId, String profession, Integer totalQuestions,
            List<QuestionPayload> questions) {
    }

    public record QuestionPayload(Long questionId, String text, List<OptionPayload> options) {
    }

    public record OptionPayload(Long optionId, String text) {
    }

    public record SubmitResponse(Long attemptId, Integer correctAnswers, Integer totalQuestions, Double score) {
    }
}
