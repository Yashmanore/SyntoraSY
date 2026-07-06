package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class Bill {
    private Integer id;           // PK
    private Integer sid;          // FK -> Society
    private String due_date;
    private LocalDateTime created_at;
    private String month;
    private Integer year;
}
