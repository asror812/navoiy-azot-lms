package org.example.lms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lms.dto.ApiResponse;
import org.example.lms.dto.HrDtos;
import org.example.lms.service.HrService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    @GetMapping("/tests")
    public ApiResponse listTests() {
        return ApiResponse.ok("OK", hrService.listTests());
    }

    @PostMapping("/tests")
    public ApiResponse createTest(@Valid @RequestBody HrDtos.CreateTestRequest req, Authentication auth) {
        return ApiResponse.ok("Test created", hrService.createTest(req, auth.getName()));
    }

    @PutMapping("/tests/{id}")
    public ApiResponse updateTest(@PathVariable Long id, @RequestBody HrDtos.UpdateTestRequest req) {
        return ApiResponse.ok("Test updated", hrService.updateTest(id, req));
    }

    @DeleteMapping("/tests/{id}")
    public ApiResponse deleteTest(@PathVariable Long id) {
        hrService.deleteTest(id);
        return ApiResponse.ok("Test deleted", null);
    }

    @PutMapping("/questions/{questionId}")
    public ApiResponse updateQuestion(@PathVariable Long questionId, @RequestBody HrDtos.UpdateQuestionRequest req) {
        return ApiResponse.ok("Question updated", hrService.updateQuestion(questionId, req));
    }

    @DeleteMapping("/questions/{questionId}")
    public ApiResponse deleteQuestion(@PathVariable Long questionId) {
        hrService.deleteQuestion(questionId);
        return ApiResponse.ok("Question deleted", null);
    }

    @GetMapping("/candidates")
    public ApiResponse listCandidates() {
        return ApiResponse.ok("OK", hrService.listCandidates());
    }

    @PostMapping("/candidates")
    public ApiResponse createCandidate(@Valid @RequestBody HrDtos.CreateCandidateRequest req) {
        return ApiResponse.ok("Candidate created", hrService.createCandidate(req));
    }

    @PutMapping("/candidates/{candidateId}")
    public ApiResponse updateCandidate(@PathVariable(name = "candidateId") Long candidateId,
            @RequestBody HrDtos.UpdateCandidateRequest req) {
        return ApiResponse.ok("Candidate updated", hrService.updateCandidate(candidateId, req));
    }

    @DeleteMapping("/candidates/{candidateId}")
    public ApiResponse deleteCandidate(@PathVariable Long candidateId) {
        hrService.deleteCandidate(candidateId);
        return ApiResponse.ok("Candidate deleted", null);
    }

}
