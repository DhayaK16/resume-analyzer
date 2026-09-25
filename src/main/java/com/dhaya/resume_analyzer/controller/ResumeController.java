package com.dhaya.resume_analyzer.controller;

import com.dhaya.resume_analyzer.model.AnalysisRecord;
import com.dhaya.resume_analyzer.repository.AnalysisRecordRepository;
import com.dhaya.resume_analyzer.service.AiService;
import com.dhaya.resume_analyzer.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResumeController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private AiService aiService;

    @Autowired
    private AnalysisRecordRepository repository;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription) {

        if (resume.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Resume file is empty."));
        }

        String filename = resume.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Only PDF files are supported."));
        }

        if (jobDescription == null || jobDescription.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Job description cannot be empty."));
        }

        String resumeText;
        try {
            resumeText = pdfService.extractText(resume);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Could not read the PDF. It may be corrupted or password-protected."));
        }

        if (resumeText == null || resumeText.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No readable text found in the PDF. Try a different file."));
        }

        String aiResultJson;
        try {
            aiResultJson = aiService.analyzeResume(resumeText, jobDescription);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "AI service is temporarily unavailable. Please try again in a moment."));
        }

        // Save to database
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        AnalysisRecord record = new AnalysisRecord(jobDescription, aiResultJson, filename, userEmail);
        repository.save(record);

        return ResponseEntity.ok(aiResultJson);
    }

    @GetMapping("/history")
    public List<AnalysisRecord> getHistory() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return repository.findByUserEmail(userEmail);
    }
}