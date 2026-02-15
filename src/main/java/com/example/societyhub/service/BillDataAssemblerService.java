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
        data.put("maintenance_contribution",
                String.valueOf(bill.getMaintenance_contribution()));

        data.put("housing_board_contribution",
                String.valueOf(bill.getHousing_board_contribution()));

        data.put("property_tax_contribution",
                String.valueOf(bill.getProperty_tax_contribution()));

        data.put("sinking_fund",
                String.valueOf(bill.getSinking_fund()));

        data.put("reserve_mhada_service_charge",
                String.valueOf(bill.getReserve_mhada_service_charge()));

        data.put("sub_charge",
                String.valueOf(bill.getSub_charge()));

        data.put("building_dev_fund",
                String.valueOf(bill.getBuilding_dev_fund()));

        data.put("other",
                String.valueOf(bill.getOther()));

        data.put("fine",
                String.valueOf(calc.get("fineAmount")));

        data.put("due_date", bill.getDue_date());

        data.put("month", month);
        data.put("status", status.replace("_", " "));
        data.put("current_month_total", calc.get("total").toString());
        double total = (double) calc.get("total");

        data.put("amount_due_in_words",
                BillingCalculationService.convertNumberToWords((int) total));

        data.put("bill_date", LocalDate.now().toString());
        data.put("bill_no", dbHandler.getNextBillNumber().toString());
        data.put("street", society.getStreet());
        data.put("landmark", society.getLandmark());
        data.put("locality", society.getLocality());
        data.put("city", society.getCity());
        data.put("pincode", society.getPincode());


        return data;
    }
}
