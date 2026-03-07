package com.aidetective.backend.analysis.exception;

public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(String id) {
        super("Analysis not found: " + id);
    }
}
