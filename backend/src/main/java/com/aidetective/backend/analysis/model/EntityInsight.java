package com.aidetective.backend.analysis.model;

public class EntityInsight {

    private String name;
    private String type;
    private String context;

    public EntityInsight() {
    }

    public EntityInsight(String name, String type, String context) {
        this.name = name;
        this.type = type;
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
