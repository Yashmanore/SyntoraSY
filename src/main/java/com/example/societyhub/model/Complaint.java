package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Complaint {
    private Long id;
    private Integer societyId;
    private String residentName;
    private String flatNo;
    private String subject;
    private String description;
    private String status; // PENDING / RESOLVED
    private LocalDateTime createdAt;

}
