package com.example.server.controller;

import com.example.server.dto.CBRSuggestionDTO;
import com.example.server.dto.SimilarCaseDTO;
import com.example.server.model.Judgment;
import com.example.server.model.JudgmentResponse;
import com.example.server.service.CaseBasedService;
import com.example.server.service.JudgmentService;
import com.example.server.service.RuleBasedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "api/judgment/")
@CrossOrigin(origins = "http://localhost:3000")
class JudgmentController {
    @Autowired
    JudgmentService judgmentService;

    @Autowired
    CaseBasedService caseBasedService;

    @Autowired
    RuleBasedService ruleBasedService;

    @GetMapping(value = "all")
    public String judgment() {
        return "judgment";
    }

    @GetMapping(value = "similar-cases")
    public List<SimilarCaseDTO> similarCases(@RequestBody Judgment judgment, @RequestParam int k) {
        return this.caseBasedService.retrieveTopK(judgment, k);
    }

    @PostMapping(value = "cbr-judgment")
    public CBRSuggestionDTO getCBRJudgment(@RequestBody Judgment judgment, @RequestParam int k) {
        var similarCases = this.caseBasedService.retrieveTopK(judgment, k);
        var suggestedJudgment = this.caseBasedService.suggestJudgment(judgment, similarCases);
        return new CBRSuggestionDTO(suggestedJudgment, similarCases);
    }

    @PostMapping(value = "")
    public void insertJudgment(@RequestBody Judgment judgment) {
        System.out.println("upsert judgment: " + judgment);
        this.judgmentService.insertJudgment(judgment);
    }

    @PostMapping(value = "rule-based")
    public JudgmentResponse getRuleBasedJudgment(@RequestBody Judgment judgment) {
        // Simply call the unified service method
        return ruleBasedService.processJudgment(judgment);
    }

}
