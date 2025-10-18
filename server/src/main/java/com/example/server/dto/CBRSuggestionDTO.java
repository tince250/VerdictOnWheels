package com.example.server.dto;

import com.example.server.model.Judgment;

import java.util.List;

public class CBRSuggestionDTO {

    private Judgment judgment;
    private List<SimilarCaseDTO> similarCases;

    public CBRSuggestionDTO(Judgment judgment, List<SimilarCaseDTO> similarCases) {
        this.judgment = judgment;
        this.similarCases = similarCases;
    }

    public Judgment getJudgment() {
        return judgment;
    }

    public void setJudgment(Judgment judgment) {
        this.judgment = judgment;
    }

    public List<SimilarCaseDTO> getSimilarCases() {
        return similarCases;
    }

    public void setSimilarCases(List<SimilarCaseDTO> similarCases) {
        this.similarCases = similarCases;
    }
}


