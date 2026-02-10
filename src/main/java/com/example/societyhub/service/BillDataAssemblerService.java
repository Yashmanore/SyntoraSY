package com.example.societyhub.service;

import com.example.societyhub.model.Bill;
import com.example.societyhub.model.Resident;
import com.example.societyhub.model.Society;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class BillDataAssemblerService {

    private final DBHandler dbHandler;
    private final BillingCalculationService calculationService;

    public BillDataAssemblerService(
            DBHandler dbHandler,
            BillingCalculationService calculationService
    ) {
        this.dbHandler = dbHandler;
        this.calculationService = calculationService;
    }

    public Map<String, String> build(
            String mygateNo,
            String month,
            String status,
            int sid
    ) throws Exception {

        Bill bill = dbHandler.fetchBill(mygateNo, month, sid);
        Society society = dbHandler.getSocietyBySid(sid);
        Resident resident = dbHandler.getResident(mygateNo);

        Map<String, Object> calc =
                calculationService.calculate(bill, status);

        Map<String, String> data = new HashMap<>();

        data.put("name", resident.getName());
        data.put("email", resident.getEmail());
        data.put("room_no", String.valueOf(resident.getRoom_no()));
        data.put("mygateNo", resident.getMygate_no());
        data.put("society_name", society.getName());
        data.put("month", month);
        data.put("status", status.replace("_", " "));
        data.put("current_month_total", calc.get("total").toString());
        data.put("bill_date", LocalDate.now().toString());
        data.put("bill_no", dbHandler.getNextBillNumber().toString());

        return data;
    }
}
