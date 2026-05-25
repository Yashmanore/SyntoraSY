package com.example.societyhub.controller;

import com.example.societyhub.model.Flat;
import com.example.societyhub.model.FlatTenancy;
import com.example.societyhub.model.Tenant;
import com.example.societyhub.service.FlatService;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flat")
public class FlatController {

    private static final Logger Log = LogManager.getLogger(FlatController.class);

    @Autowired
    private FlatService flatService;

    // ─────────────────────────────────────────────────────────────
    //  GET  /api/flat/list  — All flats for the admin's society
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/list")
    public Map<String, Object> listFlats(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer sid = (Integer) session.getAttribute("adminSocietyId");
            if (sid == null) {
                response.put("success", false);
                response.put("error", "Admin society ID not found in session");
                return response;
            }

            List<Flat> flats = flatService.getFlatsBySociety(sid);
            response.put("success", true);
            response.put("flats", flats);
        } catch (Exception e) {
            Log.error("Error listing flats", e);
            response.put("success", false);
            response.put("error", "Error fetching flats: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  GET  /api/flat/{flatId}  — Single flat details
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{flatId}")
    public Map<String, Object> getFlatDetails(@PathVariable int flatId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Flat flat = flatService.getFlatById(flatId);
            if (flat == null) {
                response.put("success", false);
                response.put("error", "Flat not found");
                return response;
            }
            response.put("success", true);
            response.put("flat", flat);
        } catch (Exception e) {
            Log.error("Error fetching flat details", e);
            response.put("success", false);
            response.put("error", "Error fetching flat: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  GET  /api/flat/my  — Flat for the logged-in resident
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/my")
    public Map<String, Object> getMyFlat(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String mygateNo = (String) session.getAttribute("residentMygate");
            if (mygateNo == null) {
                response.put("success", false);
                response.put("error", "Session expired. Please login again.");
                return response;
            }

            Flat flat = flatService.getFlatByMygate(mygateNo);
            if (flat == null) {
                response.put("success", false);
                response.put("error", "No flat found for your account.");
                return response;
            }

            response.put("success", true);
            response.put("flat", flat);

            // If the flat is rented, also include tenant details
            if ("TENANT".equalsIgnoreCase(flat.getOccupancy_type())) {
                Map<String, Object> tenantDetails = flatService.getTenantDetails(flat.getFlat_id());
                response.put("tenant_details", tenantDetails);
            }
        } catch (Exception e) {
            Log.error("Error fetching resident flat", e);
            response.put("success", false);
            response.put("error", "Error: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  POST /api/flat/add  — Add a new flat (admin action)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/add")
    public Map<String, Object> addFlat(@RequestBody Map<String, String> formData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer sid = (Integer) session.getAttribute("adminSocietyId");
            if (sid == null) {
                response.put("success", false);
                response.put("error", "Admin society ID not found in session");
                return response;
            }

            Flat flat = new Flat();
            flat.setFlat_no(formData.get("flat_no"));
            flat.setSociety_id(sid);
            flat.setOwner_mem_id(formData.get("owner_mem_id"));
            flat.setOccupancy_type(formData.getOrDefault("occupancy_type", "OWNER"));
            flat.setMygate_no(formData.get("mygate_no"));

            int flatId = flatService.addFlat(flat);

            Log.info("Flat added: flat_no=" + flat.getFlat_no() + " flatId=" + flatId);

            response.put("success", true);
            response.put("message", "Flat added successfully!");
            response.put("flat_id", flatId);
        } catch (Exception e) {
            Log.error("Error adding flat", e);
            response.put("success", false);
            response.put("error", "Error adding flat: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  PUT  /api/flat/{flatId}/occupancy  — Toggle occupancy type
    //  Body: { "occupancy_type": "OWNER" }   or   "TENANT"
    //
    //  When switching to OWNER, this also ends any active tenancy.
    //  When switching to TENANT, use the /assign-tenant endpoint.
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/{flatId}/occupancy")
    public Map<String, Object> updateOccupancy(@PathVariable int flatId,
                                                @RequestBody Map<String, String> body,
                                                HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Verify the caller owns this flat
            String mygateNo = (String) session.getAttribute("residentMygate");
            Integer adminSid = (Integer) session.getAttribute("adminSocietyId");

            if (mygateNo == null && adminSid == null) {
                response.put("success", false);
                response.put("error", "Unauthorized. Please login.");
                return response;
            }

            // Data-level security: verify ownership
            Flat flat = flatService.getFlatById(flatId);
            if (flat == null) {
                response.put("success", false);
                response.put("error", "Flat not found");
                return response;
            }

            // If resident, ensure they own this flat
            if (mygateNo != null && !mygateNo.equals(flat.getMygate_no())) {
                response.put("success", false);
                response.put("error", "You are not the owner of this flat.");
                return response;
            }

            String occupancyType = body.get("occupancy_type");
            if (occupancyType == null || (!occupancyType.equals("OWNER") && !occupancyType.equals("TENANT"))) {
                response.put("success", false);
                response.put("error", "Invalid occupancy_type. Must be 'OWNER' or 'TENANT'.");
                return response;
            }

            if ("OWNER".equals(occupancyType)) {
                // Revert to owner: end active tenancy + update flat
                flatService.revertToOwner(flatId);
                Log.info("Flat " + flatId + " reverted to OWNER occupancy.");
            } else {
                // Just update the type; tenant details should be submitted via /assign-tenant
                flatService.updateOccupancyType(flatId, "TENANT");
            }

            response.put("success", true);
            response.put("message", "Occupancy updated to " + occupancyType);
        } catch (Exception e) {
            Log.error("Error updating occupancy", e);
            response.put("success", false);
            response.put("error", "Error updating occupancy: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  POST /api/flat/{flatId}/assign-tenant  — Full tenant assignment
    //  Body: {
    //      "tenant_name": "...",
    //      "tenant_contact": "...",
    //      "lease_start_date": "2026-05-01",
    //      "lease_end_date": "2027-04-30",      (optional)
    //      "security_deposit": "50000",          (optional)
    //      "rent_amount": "15000"                (optional)
    //  }
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/{flatId}/assign-tenant")
    public Map<String, Object> assignTenant(@PathVariable int flatId,
                                             @RequestBody Map<String, String> body,
                                             HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Auth check
            String mygateNo = (String) session.getAttribute("residentMygate");
            Integer adminSid = (Integer) session.getAttribute("adminSocietyId");

            if (mygateNo == null && adminSid == null) {
                response.put("success", false);
                response.put("error", "Unauthorized. Please login.");
                return response;
            }

            // Data-level security
            Flat flat = flatService.getFlatById(flatId);
            if (flat == null) {
                response.put("success", false);
                response.put("error", "Flat not found");
                return response;
            }

            if (mygateNo != null && !mygateNo.equals(flat.getMygate_no())) {
                response.put("success", false);
                response.put("error", "You are not the owner of this flat.");
                return response;
            }

            // Build Tenant
            Tenant tenant = new Tenant();
            tenant.setName(body.get("tenant_name"));
            tenant.setContact_no(body.get("tenant_contact"));

            // Build FlatTenancy
            FlatTenancy tenancy = new FlatTenancy();
            tenancy.setFlat_id(flatId);

            String leaseStart = body.get("lease_start_date");
            tenancy.setLease_start_date(leaseStart != null ? LocalDate.parse(leaseStart) : LocalDate.now());

            String leaseEnd = body.get("lease_end_date");
            tenancy.setLease_end_date(leaseEnd != null && !leaseEnd.isEmpty() ? LocalDate.parse(leaseEnd) : null);

            String deposit = body.get("security_deposit");
            tenancy.setSecurity_deposit(deposit != null && !deposit.isEmpty() ? new BigDecimal(deposit) : BigDecimal.ZERO);

            String rent = body.get("rent_amount");
            tenancy.setRent_amount(rent != null && !rent.isEmpty() ? new BigDecimal(rent) : BigDecimal.ZERO);

            // Execute transactional assignment
            flatService.assignTenantToFlat(flatId, tenant, tenancy);

            Log.info("Tenant '" + tenant.getName() + "' assigned to flat " + flatId);

            response.put("success", true);
            response.put("message", "Tenant assigned successfully!");
        } catch (Exception e) {
            Log.error("Error assigning tenant", e);
            response.put("success", false);
            response.put("error", "Error assigning tenant: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  GET  /api/flat/{flatId}/tenant-details  — Current tenant info
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{flatId}/tenant-details")
    public Map<String, Object> getTenantDetails(@PathVariable int flatId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> details = flatService.getTenantDetails(flatId);
            if (details == null) {
                response.put("success", true);
                response.put("message", "No active tenant for this flat.");
                response.put("tenant_details", null);
            } else {
                response.put("success", true);
                response.put("tenant_details", details);
            }
        } catch (Exception e) {
            Log.error("Error fetching tenant details", e);
            response.put("success", false);
            response.put("error", "Error: " + e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    //  PUT  /api/flat/{flatId}/revert-to-owner  — End tenancy
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/{flatId}/revert-to-owner")
    public Map<String, Object> revertToOwner(@PathVariable int flatId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Auth check
            String mygateNo = (String) session.getAttribute("residentMygate");
            Integer adminSid = (Integer) session.getAttribute("adminSocietyId");

            if (mygateNo == null && adminSid == null) {
                response.put("success", false);
                response.put("error", "Unauthorized. Please login.");
                return response;
            }

            // Data-level security
            Flat flat = flatService.getFlatById(flatId);
            if (flat == null) {
                response.put("success", false);
                response.put("error", "Flat not found");
                return response;
            }

            if (mygateNo != null && !mygateNo.equals(flat.getMygate_no())) {
                response.put("success", false);
                response.put("error", "You are not the owner of this flat.");
                return response;
            }

            flatService.revertToOwner(flatId);

            Log.info("Flat " + flatId + " reverted to OWNER occupancy.");

            response.put("success", true);
            response.put("message", "Flat reverted to owner-occupied successfully.");
        } catch (Exception e) {
            Log.error("Error reverting to owner", e);
            response.put("success", false);
            response.put("error", "Error: " + e.getMessage());
        }
        return response;
    }
}
