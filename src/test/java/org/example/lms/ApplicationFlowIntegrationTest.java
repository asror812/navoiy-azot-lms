package org.example.lms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportHrCandidateExamAndResultsFlow() throws Exception {
        String profession = "qa-engineer";

        mockMvc.perform(post("/api/hr/jobs")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("hr", "hr123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "qa-engineer",
                                  "description": "QA role"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/hr/tests")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("hr", "hr123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "QA Basics",
                                  "profession": "qa-engineer",
                                  "questionText": "What is regression testing?",
                                  "options": [
                                    {"text":"Testing old features after changes","correct":true},
                                    {"text":"Only UI color testing","correct":false}
                                  ],
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult createCandidate = mockMvc.perform(post("/api/hr/candidates")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("hr", "hr123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alice Doe",
                                  "profession": "qa-engineer",
                                  "login": "AA1111111",
                                  "password": "AA1111111",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode candidateJson = objectMapper.readTree(createCandidate.getResponse().getContentAsString());
        long candidateId = candidateJson.get("data").get("candidateId").asLong();

        MvcResult login = mockMvc.perform(post("/api/candidate/auth/passport-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alice Doe",
                                  "passport": "AA1111111"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(login.getResponse().getContentAsString());
        assertThat(loginJson.get("data").get("candidateId").asLong()).isEqualTo(candidateId);

        MvcResult start = mockMvc.perform(post("/api/candidate/tests/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateId": %d
                                }
                                """.formatted(candidateId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode startJson = objectMapper.readTree(start.getResponse().getContentAsString());
        long attemptId = startJson.get("data").get("attemptId").asLong();
        long questionId = startJson.get("data").get("questions").get(0).get("questionId").asLong();
        long optionId = startJson.get("data").get("questions").get(0).get("options").get(0).get("optionId").asLong();

        mockMvc.perform(post("/api/candidate/attempts/{attemptId}/progress", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateId": %d,
                                  "answers": [
                                    {
                                      "questionId": %d,
                                      "selectedOptionId": %d
                                    }
                                  ]
                                }
                                """.formatted(candidateId, questionId, optionId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/candidate/attempts/{attemptId}/submit", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateId": %d,
                                  "answers": [
                                    {
                                      "questionId": %d,
                                      "selectedOptionId": %d
                                    }
                                  ]
                                }
                                """.formatted(candidateId, questionId, optionId)))
                .andExpect(status().isOk());

        MvcResult results = mockMvc.perform(get("/api/hr/results")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("hr", "hr123"))
                        .param("job", profession)
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultsJson = objectMapper.readTree(results.getResponse().getContentAsString());
        assertThat(resultsJson.get("data").isArray()).isTrue();
        assertThat(resultsJson.get("data").size()).isGreaterThan(0);
    }

    @Test
    void shouldUpdateCandidatePassport() throws Exception {
        MvcResult createCandidate = mockMvc.perform(post("/api/hr/candidates")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("hr", "hr123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Bob Test",
                                  "profession": "mechanic",
                                  "login": "BB1111111",
                                  "password": "BB1111111",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        long candidateId = objectMapper.readTree(createCandidate.getResponse().getContentAsString())
                .get("data").get("candidateId").asLong();

        mockMvc.perform(put("/api/hr/candidates/{candidateId}/passport", candidateId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("hr", "hr123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passport": "BB2222222"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/candidate/auth/passport-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Bob Test",
                                  "passport": "BB2222222"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
