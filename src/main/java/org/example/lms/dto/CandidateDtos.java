package org.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CandidateDtos {

        public record LoginRequest(
                        @NotBlank String login,
                        @NotBlank String password) {
        }

        public record StartTestRequest(@NotNull Long candidateId) {
        }

        public record SubmitAttemptRequest(
                        @NotNull Long candidateId,
                        @NotNull List<AnswerRequest> answers) {
        }

        public record AnswerRequest(
                        @NotNull Long questionId,
                        Long selectedOptionId) {
        }
}
