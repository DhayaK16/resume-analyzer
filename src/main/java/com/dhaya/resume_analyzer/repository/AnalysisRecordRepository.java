package com.dhaya.resume_analyzer.repository;

import com.dhaya.resume_analyzer.model.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {
    List<AnalysisRecord> findByUserEmail(String userEmail);
}