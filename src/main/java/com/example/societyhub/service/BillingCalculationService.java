package com.example.societyhub.service;

import com.example.societyhub.model.Bill;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BillingCalculationService {

    public Map<String, Object> calculate(Bill bill, String status) {

        double baseTotal =
                bill.getMaintenance_contribution()
                        + bill.getHousing_board_contribution()
                        + bill.getProperty_tax_contribution()
                        + bill.getSinking_fund()
                        + bill.getReserve_mhada_service_charge()
                        + bill.getSub_charge()
                        + bill.getOther()
                        + bill.getBuilding_dev_fund();

        double fineAmount = bill.getFine();

        String normalized =
                status == null ? "" :
                        status.trim()
                                .toLowerCase()
                                .replace(" ", "_");

        double finalTotal = baseTotal;
        double finalFine = 0.0;

        if (normalized.equals("paid_with_fine")) {
            finalTotal += fineAmount;
            finalFine = fineAmount;
        }

        // If status is only "paid" or anything else,
        // fine remains 0 and total remains baseTotal

        Map<String, Object> result = new HashMap<>();
        result.put("total", finalTotal);
        result.put("fineAmount", finalFine);

        return result;
    }

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
