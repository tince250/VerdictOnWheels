package com.example.server.dto;

import com.example.server.model.Judgment;

public class SimilarCaseDTO {
    private Judgment caseDesc;
    private double similarity;

    public SimilarCaseDTO(Judgment caseDesc, double similarity) {
        this.caseDesc = caseDesc;
        this.similarity = similarity;
    }

    public Judgment getCaseDesc() {
        return caseDesc;
    }

    public void setCaseDesc(Judgment caseDesc) {
        this.caseDesc = caseDesc;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return "SimilarCaseDTO{" +
                "caseDesc=" + caseDesc +
                ", similarity=" + similarity +
                '}';
    }
}
