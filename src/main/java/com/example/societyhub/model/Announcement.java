package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Announcement {
    private Integer id;            // PK
    private Integer sid;           // FK -> Society
    private String title;
    private String message;
    private String category;
    private Boolean isActive;
    private LocalDateTime createdAt = LocalDateTime.now();
}
