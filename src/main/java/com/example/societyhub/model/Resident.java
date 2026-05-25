package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Resident {
    private String mem_id;   // PK
    private Integer age;
    private String contact_no;
    private Boolean is_admin;
    private String mygate_no;
    private String bhk;
    private String email;
    private String password;
    
    private String name;
    private String room_no;
    private String mr_ms;
    private String gender;
    
    private Boolean is_tenant;
    private Tenant tenant;

    // Billing status populated from unit_bill_record for the selected month.
    // Defaults to "Unpaid" if no bill record exists yet.
    private String status = "Unpaid";
}
