package com.aidetective.backend.analysis.model;

public enum Verdict {
    LIKELY_FAKE("Likely Fake"),
    QUESTIONABLE("Questionable"),
    LIKELY_TRUE("Likely True"),
    VERIFIED("Verified");

    private final String label;

    Verdict(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Verdict fromScore(int score) {
        if (score < 30) {
            return LIKELY_FAKE;
        }
        if (score < 60) {
            return QUESTIONABLE;
        }
        if (score < 80) {
            return LIKELY_TRUE;
        }
        return VERIFIED;
    }

    public static Verdict fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        for (Verdict verdict : values()) {
            if (verdict.label.equalsIgnoreCase(label.trim())) {
                return verdict;
            }
        }
        return null;
    }
}
