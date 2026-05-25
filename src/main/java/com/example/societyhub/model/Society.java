package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Society {
    private Integer sid;          // PK
    private String name;
    private String street;
    private String landmark;
    private String locality;
    private String pincode;
    private String city;
    private String state;
    private String country;
    private Integer admin_id;
    private Boolean data_uploaded; // new field from schema
}