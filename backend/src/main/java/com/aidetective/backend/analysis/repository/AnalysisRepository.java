package com.aidetective.backend.analysis.repository;

import com.aidetective.backend.analysis.model.AnalysisRecord;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnalysisRepository extends MongoRepository<AnalysisRecord, String> {

    java.util.List<AnalysisRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<AnalysisRecord> findById(String id);
}
