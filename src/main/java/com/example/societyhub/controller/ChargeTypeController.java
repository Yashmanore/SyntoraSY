package com.example.societyhub.controller;

import com.example.societyhub.model.Bill;
import com.example.societyhub.model.ChargeType;
import com.example.societyhub.service.BillingService;
import com.example.societyhub.service.ChargeTypeService;
import com.example.societyhub.service.DBHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing charge types (CRUD) and triggering bill generation.
 */
@Controller
@RequestMapping("/api/charges")
public class ChargeTypeController {

    private final ChargeTypeService chargeTypeService;
    private final BillingService billingService;
    private final DBHandler dbHandler;

    @Autowired
    public ChargeTypeController(ChargeTypeService chargeTypeService,
                                 BillingService billingService,
                                 DBHandler dbHandler) {
        this.chargeTypeService = chargeTypeService;
        this.billingService = billingService;
        this.dbHandler = dbHandler;
    }

    // ─── CHARGE TYPE CRUD (REST + Thymeleaf compatible) ─────────────────────────

    /**
     * List all charge types for the admin's society.
     */
    @GetMapping("")
    public String listChargeTypes(Model model, HttpSession session) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            return "redirect:/login";
        }

        try {
            List<ChargeType> chargeTypes = chargeTypeService.getAllChargeTypesBySociety(sid);
            model.addAttribute("chargeTypes", chargeTypes);
            model.addAttribute("adminSocietyId", sid);
            model.addAttribute("role", "admin");

            // Load existing bill contributions for this society
            Bill bill = dbHandler.fetchBillDetails(sid);
            if (bill == null) {
                bill = new Bill(); // empty defaults (all null -> 0 in template)
            }
            model.addAttribute("bill", bill);

            return "admin/bill_form";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load charge types");
            return "error";
        }
    }

    /**
     * Add a new charge type.
     */
    @PostMapping("/add")
    public String addChargeType(@RequestParam String name,
                                 @RequestParam BigDecimal defaultAmount,
                                 @RequestParam String applicableTo,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            return "error";
        }

        try {
            ChargeType ct = new ChargeType();
            ct.setSociety_id(sid);
            ct.setName(name);
            ct.setDefault_amount(defaultAmount);
            ct.setApplicable_to(applicableTo.toUpperCase());
            ct.setIs_active(true);

            chargeTypeService.addChargeType(ct);
            redirectAttributes.addFlashAttribute("success", "Charge type '" + name + "' added successfully");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to add charge type: " + e.getMessage());
        }

        return "redirect:/api/charges";
    }

    /**
     * Update an existing charge type.
     */
    @PostMapping("/update/{id}")
    public String updateChargeType(@PathVariable int id,
                                    @RequestParam String name,
                                    @RequestParam BigDecimal defaultAmount,
                                    @RequestParam String applicableTo,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            return "error";
        }

        try {
            ChargeType ct = chargeTypeService.getChargeTypeById(id);
            if (ct == null || !ct.getSociety_id().equals(sid)) {
                redirectAttributes.addFlashAttribute("error", "Charge type not found");
                return "redirect:/api/charges";
            }

            ct.setName(name);
            ct.setDefault_amount(defaultAmount);
            ct.setApplicable_to(applicableTo.toUpperCase());
            chargeTypeService.updateChargeType(ct);

            redirectAttributes.addFlashAttribute("success", "Charge type updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to update: " + e.getMessage());
        }

        return "redirect:/api/charges";
    }

    /**
     * Deactivate (soft-delete) a charge type.
     */
    @PostMapping("/deactivate/{id}")
    public String deactivateChargeType(@PathVariable int id,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            return "error";
        }

        try {
            chargeTypeService.deactivateChargeType(id);
            redirectAttributes.addFlashAttribute("success", "Charge type deactivated");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate: " + e.getMessage());
        }

        return "redirect:/api/charges";
    }

    /**
     * Re-activate a charge type.
     */
    @PostMapping("/activate/{id}")
    public String activateChargeType(@PathVariable int id,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            return "error";
        }

        try {
            chargeTypeService.activateChargeType(id);
            redirectAttributes.addFlashAttribute("success", "Charge type re-activated");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to activate: " + e.getMessage());
        }

        return "redirect:/api/charges";
    }

    // ─── BILL GENERATION ────────────────────────────────────────────────────────

    /**
     * Save static bill contribution fields (admin only).
     */
    @PostMapping("/update-contributions")
    public String updateContributions(
            @RequestParam(defaultValue = "0") Integer maintenance_contribution,
            @RequestParam(defaultValue = "0") Integer housing_board_contribution,
            @RequestParam(defaultValue = "0") Integer property_tax_contribution,
            @RequestParam(defaultValue = "0") Integer sinking_fund,
            @RequestParam(defaultValue = "0") Integer reserve_mhada_service_charge,
            @RequestParam(defaultValue = "0") Integer sub_charge,
            @RequestParam(defaultValue = "0") Integer fine,
            @RequestParam(defaultValue = "0") Integer building_dev_fund,
            @RequestParam(defaultValue = "0") Integer other,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Integer memId = (Integer) session.getAttribute("adminMemId");
        if (sid == null || memId == null || !billingService.isAdmin(memId)) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized. Only admins can update contributions.");
            return "redirect:/api/charges";
        }

        try {
            Bill bill = new Bill();
            bill.setMaintenance_contribution(maintenance_contribution);
            bill.setHousing_board_contribution(housing_board_contribution);
            bill.setProperty_tax_contribution(property_tax_contribution);
            bill.setSinking_fund(sinking_fund);
            bill.setReserve_mhada_service_charge(reserve_mhada_service_charge);
            bill.setSub_charge(sub_charge);
            bill.setFine(fine);
            bill.setBuilding_dev_fund(building_dev_fund);
            bill.setOther(other);

            dbHandler.updateBillContributions(sid, bill);
            redirectAttributes.addFlashAttribute("success", "Bill contributions saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to save contributions: " + e.getMessage());
        }

        return "redirect:/api/charges";
    }

    /**
     * Generate monthly bills for the society.
     * This triggers the occupancy-aware billing engine.
     */
    @PostMapping("/generate-bills")
    public String generateBills(@RequestParam String month,
                                 @RequestParam int year,
                                 @RequestParam String dueDate,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Integer memId = (Integer) session.getAttribute("adminMemId");
        if (sid == null || memId == null || !billingService.isAdmin(memId)) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized access. Only admins can generate bills.");
            return "redirect:/login";
        }

        try {
            int billId = billingService.generateMonthlyBills(sid, month.toLowerCase(), year, dueDate);
            redirectAttributes.addFlashAttribute("success",
                    "Bills generated successfully for " + month + " " + year + " (Bill #" + billId + ")");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Failed to generate bills: " + e.getMessage());
        }

        return "redirect:/api/charges";
    }

    /**
     * REST AJAX: Generate monthly bills and return JSON (used by generate_bill.html).
     */
    @PostMapping("/generate-bills-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateBillsAjax(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Integer memId = (Integer) session.getAttribute("adminMemId");
        if (sid == null || memId == null || !billingService.isAdmin(memId)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Unauthorized. Only admins are allowed to generate bills."));
        }

        try {
            String month   = ((String) payload.get("month")).toLowerCase();
            int    year    = Integer.parseInt(payload.get("year").toString());
            String dueDate = (String) payload.get("dueDate");

            int billId = billingService.generateMonthlyBills(sid, month, year, dueDate);

            String display = month.substring(0, 1).toUpperCase() + month.substring(1);
            return ResponseEntity.ok(
                    Map.of("message",
                            "Bills generated successfully for " + display + " " + year +
                            " (Bill #" + billId + ")"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ─── REST API ENDPOINTS (for AJAX / future frontend) ────────────────────────

    /**
     * REST: Get all charge types for a society as JSON.
     */
    @GetMapping("/api/list")
    @ResponseBody
    public List<ChargeType> getChargeTypesJson(HttpSession session) throws Exception {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            throw new RuntimeException("No society ID in session");
        }
        return chargeTypeService.getAllChargeTypesBySociety(sid);
    }

    /**
     * REST: Get applicable charges for a specific occupancy type.
     */
    @GetMapping("/api/applicable")
    @ResponseBody
    public List<ChargeType> getApplicableChargesJson(
            @RequestParam String occupancyType,
            HttpSession session) throws Exception {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        if (sid == null) {
            throw new RuntimeException("No society ID in session");
        }
        return chargeTypeService.getApplicableChargeTypes(sid, occupancyType);
    }
}
