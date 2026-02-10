package com.example.societyhub.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String languagePage() {
        return "language";
    }
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    @GetMapping("/home")
    public String home() {
        System.out.println(encoder.encode("secureHub$18"));
        return "home";
    }
}

