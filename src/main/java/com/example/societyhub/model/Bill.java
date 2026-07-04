package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class Bill {
    private Integer id;           // PK
    private Integer sid;          // FK -> Society
    private String due_date;
    private String month;
    private Integer year;

    // Static contribution fields (stored in bill table)
    private Integer maintenance_contribution;
    private Integer housing_board_contribution;
    private Integer property_tax_contribution;
    private Integer sinking_fund;
    private Integer reserve_mhada_service_charge;
    private Integer sub_charge;
    private Integer fine;
    private Integer building_dev_fund;
    private Integer other;
}
