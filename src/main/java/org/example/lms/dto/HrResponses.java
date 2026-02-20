package org.example.lms.dto;

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
            String password,
            Boolean active) {
    }
}
