package org.example.lms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lms.dto.ApiResponse;
import org.example.lms.dto.CandidateDtos;
import org.example.lms.service.CandidateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping("/auth/login")
    public ApiResponse login(@Valid @RequestBody CandidateDtos.LoginRequest req) {
        return ApiResponse.ok("Login success", candidateService.login(req));
    }

    @GetMapping("/{candidateId}/tests")
    public ApiResponse listRandomTests(@PathVariable Long candidateId) {
        return ApiResponse.ok("OK", candidateService.listRandomTests(candidateId));
    }

    @PostMapping("/tests/start")
    public ApiResponse start(@Valid @RequestBody CandidateDtos.StartTestRequest req) {
        return ApiResponse.ok("Started", candidateService.startTest(req));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse submit(@PathVariable Long attemptId,
            @Valid @RequestBody CandidateDtos.SubmitAttemptRequest req) {
        return ApiResponse.ok("Submitted", candidateService.submitAttempt(attemptId, req));
    }
}
