package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Flat {
    private Integer flat_id;      // PK
    private String flat_no;
    private Integer society_id;   // FK -> Society
    private String owner_mem_id;  // FK -> Resident
    private String occupancy_type; // ENUM ('OWNER', 'TENANT')
    private String mygate_no;
}
