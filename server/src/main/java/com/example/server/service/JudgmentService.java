package com.example.server.service;

import com.example.server.model.Judgment;
import com.example.server.repository.CsvConnector;
import org.springframework.stereotype.Service;

@Service
public class JudgmentService {

    private CsvConnector csvConnector;

    public JudgmentService(CsvConnector csvConnector) {
        this.csvConnector = csvConnector;
    }

    public void insertJudgment(Judgment judgment) {
        this.csvConnector.upsert(judgment);
    }

}
