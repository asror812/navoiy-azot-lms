package org.example.lms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lms.dto.ApiResponse;
import org.example.lms.dto.HrDtos;
import org.example.lms.service.HrService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    @PutMapping("/candidates/{candidateId}/passport")
    public ApiResponse updateCandidatePassport(@PathVariable(name = "candidateId") Long candidateId,
            @Valid @RequestBody HrDtos.UpdateCandidatePassportRequest req) {
        return ApiResponse.ok("Candidate passport updated", hrService.updateCandidatePassport(candidateId, req));
    }

    @DeleteMapping("/candidates/{candidateId}")
    public ApiResponse deleteCandidate(@PathVariable Long candidateId) {
        hrService.deleteCandidate(candidateId);
        return ApiResponse.ok("Candidate deleted", null);
    }

    @GetMapping("/jobs")
    public ApiResponse listJobs() {
        return ApiResponse.ok("OK", hrService.listJobs());
    }

    @PostMapping("/jobs")
    public ApiResponse createJob(@Valid @RequestBody HrDtos.CreateJobRequest req) {
        return ApiResponse.ok("Job created", hrService.createJob(req));
    }

    @PutMapping("/jobs/{jobId}")
    public ApiResponse updateJob(@PathVariable Long jobId, @RequestBody HrDtos.UpdateJobRequest req) {
        return ApiResponse.ok("Job updated", hrService.updateJob(jobId, req));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ApiResponse deleteJob(@PathVariable Long jobId) {
        hrService.deleteJob(jobId);
        return ApiResponse.ok("Job deleted", null);
    }

    @GetMapping("/results")
    public ApiResponse listResults(
            @RequestParam(required = false) String job,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String candidate,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Double maxScore,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok("OK", hrService.listResults(job, fromDate, toDate, candidate, minScore, maxScore, status));
    }
}
