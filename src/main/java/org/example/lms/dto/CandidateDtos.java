package org.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CandidateDtos {

        public record LoginRequest(
                        @NotBlank(message = "login is required") String login,
                        @NotBlank(message = "password is required") String password) {
        }

        public record PassportLoginRequest(
                        @NotBlank(message = "fullName is required") String fullName,
                        @NotBlank(message = "passport is required") String passport) {
        }

        public record StartTestRequest(@NotNull(message = "candidateId is required") Long candidateId) {
        }

        public record SubmitAttemptRequest(
                        @NotNull(message = "candidateId is required") Long candidateId,
                        @NotNull(message = "answers are required") List<AnswerRequest> answers) {
        }

        public record SaveProgressRequest(
                        @NotNull(message = "candidateId is required") Long candidateId,
                        @NotNull(message = "answers are required") List<AnswerRequest> answers) {
        }

        public record AnswerRequest(
                        @NotNull(message = "questionId is required") Long questionId,
                        Long selectedOptionId) {
        }
}
