package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Admin {
    // Getters and setters
    private Integer mem_id;
    private String name;
    private String contact_no;
    private String email_id;
    private String adminPassword;
    private Integer societyId;
    private String mygate_no;
}
