package org.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class HrDtos {

        public record CreateTestRequest(
                        @NotBlank(message = "title is required") String title,
                        @NotBlank(message = "profession is required") String profession,
                        @NotBlank(message = "question text is required") String questionText,
                        @NotEmpty(message = "options must not be empty")
                        @Size(min = 2, message = "at least 2 options are required")
                        List<@NotNull(message = "option must not be null") OptionRequest> options,
                        Boolean active) {
        }

        public record UpdateTestRequest(
                        String title,
                        String profession,
                        String questionText,
                        Boolean active) {
        }

        public record CreateQuestionRequest(
                        @NotBlank(message = "question text is required") String text,
                        @NotEmpty(message = "options must not be empty") List<@NotNull(message = "option must not be null") OptionRequest> options) {
        }

        public record UpdateQuestionRequest(
                        String text,
                        List<OptionRequest> options) {
        }

        public record OptionRequest(
                        @NotBlank(message = "option text is required") String text,
                        @NotNull(message = "option correct flag is required") Boolean correct) {
        }

        public record CreateCandidateRequest(
                        @NotBlank(message = "fullName is required") String fullName,
                        @NotBlank(message = "profession is required") String profession,
                        @NotBlank(message = "login is required") String login,
                        @NotBlank(message = "password is required") String password,
                        Boolean active) {
        }

        public record UpdateCandidateRequest(
                        String fullName,
                        String profession,
                        String password,
                        Boolean active) {
        }
}
