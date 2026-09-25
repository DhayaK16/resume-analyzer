package com.dhaya.resume_analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public String analyzeResume(String resumeText, String jobDescription) {
        String prompt = """
            You are a resume reviewer. Compare the resume against the job description.
            Respond ONLY with valid JSON in this exact format, no markdown formatting, no extra text, no code fences:
            {
              "matchScore": 0,
              "missingKeywords": ["keyword1"],
              "strengths": ["strength1"],
              "improvements": ["suggestion1"]
            }

            RESUME:
            %s

            JOB DESCRIPTION:
            %s
            """.formatted(resumeText, jobDescription);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key="
                + apiKey;

        Map response = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<Map> candidates = (List<Map>) response.get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        String rawText = (String) parts.get(0).get("text");

        // Gemini sometimes wraps JSON in ```json ... ``` code fences — strip them if present
        return rawText.replaceAll("```json", "").replaceAll("```", "").trim();
    }
}