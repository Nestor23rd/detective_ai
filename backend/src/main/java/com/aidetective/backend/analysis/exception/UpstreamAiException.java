package com.aidetective.backend.analysis.exception;

public class UpstreamAiException extends RuntimeException {

    public UpstreamAiException(String message) {
        super(message);
    }

    public UpstreamAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
