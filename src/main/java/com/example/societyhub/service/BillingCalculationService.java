package com.example.societyhub.service;

import com.example.societyhub.model.UnitBillRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillingCalculationService {

    private final BillingService billingService;

    @Autowired
    public BillingCalculationService(@Lazy BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Calculate totals for a specific flat's bill (UnitBillRecord).
     * Sums all BillLineItem amounts from the DB via BillingService.
     */
    public Map<String, Object> calculateForUnitBill(int unitBillRecordId, String status) throws SQLException {
        BigDecimal baseTotal = billingService.calculateTotalForUnitBill(unitBillRecordId);
        BigDecimal fineAmount = BigDecimal.ZERO;

        String normalized = status == null ? "" :
                status.trim().toLowerCase().replace(" ", "_");

        BigDecimal finalTotal = baseTotal;
        if ("paid_with_fine".equals(normalized)) {
            // Fine logic: configurable in the future; default 10% for now
            fineAmount = baseTotal.multiply(new BigDecimal("0.10"));
            finalTotal = finalTotal.add(fineAmount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", finalTotal);
        result.put("baseTotal", baseTotal);
        result.put("fineAmount", fineAmount);

        return result;
    }

    /**
     * Get a full bill breakdown for a specific flat (charges + totals).
     */
    public Map<String, Object> getFullBillBreakdown(int flatId, String month, int year) throws SQLException {
        return billingService.getBillSummaryForFlat(flatId, month, year);
    }

    /**
     * Calculate totals for all flats in a society for a billing cycle.
     * Returns a list of maps, each containing flat_id, total, status, etc.
     */
    public List<UnitBillRecord> getSocietyBillSummary(int societyId, String month, int year) throws SQLException {
        return billingService.getUnitBillRecordsBySociety(societyId, month, year);
    }

    // ─── UTILITY METHODS (preserved) ────────────────────────────────────────────

    public static String convertNumberToWords(int number) {
        if (number == 0) {
            return "Zero";
        }

        String[] units = {
                "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
                "Seventeen", "Eighteen", "Nineteen"
        };

        String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        };

        String[] thousands = {
                "", "Thousand", "Million", "Billion"
        };

        String words = "";
        int i = 0;

        while (number > 0) {
            if (number % 1000 != 0) {
                words = convertHundreds(number % 1000, units, tens) + thousands[i] + " " + words;
            }
            number /= 1000;
            i++;
        }

        return words.trim();
    }

    public static String convertHundreds(int number, String[] units, String[] tens) {
        String words = "";

        if (number >= 100) {
            words += units[number / 100] + " Hundred ";
            number %= 100;
        }

        if (number >= 20) {
            words += tens[number / 10] + " ";
            number %= 10;
        }

        if (number > 0) {
            words += units[number] + " ";
        }

        return words;
    }

    public double parseDoubleSafely(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
