package com.example.societyhub.service;

import com.example.societyhub.model.Bill;
import com.example.societyhub.model.Flat;
import com.example.societyhub.model.Resident;
import com.example.societyhub.model.Society;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillDataAssemblerService {

    private final DBHandler dbHandler;
    private final BillingCalculationService calculationService;
    private final FlatService flatService;
    private final BillingService billingService;

    public BillDataAssemblerService(
            DBHandler dbHandler,
            BillingCalculationService calculationService,
            FlatService flatService,
            BillingService billingService
    ) {
        this.dbHandler = dbHandler;
        this.calculationService = calculationService;
        this.flatService = flatService;
        this.billingService = billingService;
    }

    public Map<String, Object> build(
            String mygateNo,
            String month,
            String status,
            int sid
    ) throws Exception {

        // Normalise month: DB stores lowercase (e.g. "january")
        String monthLower = (month != null && !month.isEmpty())
                ? month.toLowerCase() : "";

        Bill bill = dbHandler.fetchBill(mygateNo, monthLower, sid);
        Society society = dbHandler.getSocietyBySid(sid);
        Resident resident = dbHandler.getResident(mygateNo);
        Flat flat = flatService.getFlatByMygate(mygateNo);

        Map<String, Object> data = new HashMap<>();

        // ── Resident / society info ──────────────────────────────────────────
        data.put("mem_id",           resident != null ? resident.getMem_id()   : "N/A");
        data.put("resident_name",    resident != null && resident.getName() != null ? resident.getName() : "N/A");
        data.put("email",            resident != null ? resident.getEmail()     : "");
        data.put("flat_no",      flat     != null ? flat.getFlat_no()       : "N/A");
        data.put("mygateNo",     resident != null ? resident.getMygate_no() : mygateNo);
        data.put("society_name", society  != null ? society.getName()       : "");
        data.put("street",       society  != null ? society.getStreet()     : "");
        data.put("landmark",     society  != null ? society.getLandmark()   : "");
        data.put("locality",     society  != null ? society.getLocality()   : "");
        data.put("city",         society  != null ? society.getCity()       : "");
        data.put("pincode",      society  != null ? society.getPincode()    : "");

        String occType = flat != null ? flat.getOccupancy_type() : "OWNER";
        String displayOccType = "OWNER".equalsIgnoreCase(occType) ? "OWNER" : "RENTAL";
        data.put("occupancy_type", displayOccType);

        data.put("due_date", bill != null ? bill.getDue_date() : "");

        // Display month with capital first letter
        String monthDisplay = monthLower.isEmpty() ? "" :
                monthLower.substring(0, 1).toUpperCase() + monthLower.substring(1);
        data.put("month",  monthDisplay);
        data.put("status", status != null ? status.replace("_", " ") : "");

        data.put("bill_date", LocalDate.now().toString());
        Integer billNo = dbHandler.getNextBillNumber();
        data.put("bill_no", billNo != null ? billNo.toString() : "0");

        // ── Billing amounts — always initialise so template never sees null ─
        data.put("current_month_total", "0");
        data.put("fine",                "0");
        data.put("amount_due",          "0");
        data.put("amount_due_in_words", "Zero");
        data.put("arrears",             "0");         // required by receipt.html
        data.put("bldg_fund_due",       "0");         // required by receipt.html
        data.put("lineItems",           Collections.emptyList()); // never null

        // ── Fetch real billing data from the new schema ──────────────────────
        if (flat != null && bill != null) {
            int year = bill.getYear();
            Map<String, Object> billSummary = billingService.getBillSummaryForFlat(
                    flat.getFlat_id(), monthLower, year);

            if (billSummary != null) {
                BigDecimal totalAmount = (BigDecimal) billSummary.get("total_amount");
                BigDecimal fineAmount  = (BigDecimal) billSummary.get("fine_amount");
                BigDecimal grandTotal  = (BigDecimal) billSummary.get("grand_total");

                data.put("current_month_total",
                        totalAmount != null ? totalAmount.toString() : "0");
                data.put("fine",
                        fineAmount  != null ? fineAmount.toString()  : "0");
                data.put("amount_due",
                        grandTotal  != null ? grandTotal.toString()  : "0");
                data.put("amount_due_in_words",
                        BillingCalculationService.convertNumberToWords(
                                grandTotal != null ? grandTotal.intValue() : 0));

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lineItems =
                        (List<Map<String, Object>>) billSummary.get("line_items");
                if (lineItems != null && !lineItems.isEmpty()) {
                    data.put("lineItems", lineItems);
                }
            }
        }

        return data;
    }
}
