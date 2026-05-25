package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Note {
    private Integer id;         // PK
    private Integer sid;        // FK -> Society
    private Integer society_id; // additional society reference per schema
    private String name;
    private String title;
    private String message;
}
