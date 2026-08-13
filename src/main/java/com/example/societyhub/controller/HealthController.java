package com.example.societyhub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@RestController
public class HealthController {

    private static final List<String> GROQ_MODELS = Arrays.asList(
        "llama3-8b-8192",
        "llama3-70b-8192",
        "mixtral-8x7b-32768",
        "gemma2-9b-it",
        "llama-3.1-70b-versatile",
        "llama-3.1-8b-instant"
    );

    @GetMapping("/api/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "online");
        response.put("provider", "Groq");
        response.put("models", GROQ_MODELS);
        return response;
    }
}
