package com.example.server.controller;

import com.example.server.service.JudgmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "api/judgment/")
class JudgmentController {
    @Autowired
    JudgmentService judgmentService;

    @GetMapping(value = "all")
    public String judgment() {
        return "judgment";
    }
}
