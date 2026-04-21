package com.example.server.model;

import java.util.List;
import java.util.Map;

public class JudgmentResponse {
    private String description;
    private List<String> appliedProvisions;
    private Map<String, Object> penalties;

    public JudgmentResponse() {
    }

    public JudgmentResponse(String description, List<String> appliedProvisions, Map<String, Object> penalties) {
        this.description = description;
        this.appliedProvisions = appliedProvisions;
        this.penalties = penalties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAppliedProvisions() {
        return appliedProvisions;
    }

    public void setAppliedProvisions(List<String> appliedProvisions) {
        this.appliedProvisions = appliedProvisions;
    }

    public Map<String, Object> getPenalties() {
        return penalties;
    }

    public void setPenalties(Map<String, Object> penalties) {
        this.penalties = penalties;
    }
}
