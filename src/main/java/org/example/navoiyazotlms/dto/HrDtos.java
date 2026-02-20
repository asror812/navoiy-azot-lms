package org.example.navoiyazotlms.simple.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class HrDtos {

        public record CreateTestRequest(
                        @NotBlank String title,
                        Integer minQuestionsPerAttempt,
                        Integer maxQuestionsPerAttempt,
                        Boolean active) {
        }

        public record UpdateTestRequest(
                        String title,
                        Integer minQuestionsPerAttempt,
                        Integer maxQuestionsPerAttempt,
                        Boolean active) {
        }

        public record CreateQuestionRequest(
                        @NotBlank String text,
                        @NotEmpty List<@NotNull OptionRequest> options) {
        }

        public record UpdateQuestionRequest(
                        String text,
                        List<OptionRequest> options) {
        }

        public record OptionRequest(
                        @NotBlank String text,
                        @NotNull Boolean correct) {
        }

        public record CreateCandidateRequest(
                        @NotBlank String fullName,
                        @NotBlank String login,
                        @NotBlank String password,
                        Boolean active) {
        }

        public record UpdateCandidateRequest(
                        String fullName,
                        String password,
                        Boolean active) {
        }
}
