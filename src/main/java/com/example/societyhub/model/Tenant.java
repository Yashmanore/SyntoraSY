package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Tenant {
    private Integer tenant_id;  // PK
    private String name;
    private String contact_no;
    private String email;
    private String bill_type;   // ALL, TENANT, RENTAL
}
