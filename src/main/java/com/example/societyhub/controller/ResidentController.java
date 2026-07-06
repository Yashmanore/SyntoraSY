package com.example.societyhub.controller;

import com.example.societyhub.model.Announcement;
import com.example.societyhub.model.Complaint;
import com.example.societyhub.model.Flat;
import com.example.societyhub.service.BillingService;
import com.example.societyhub.service.DBHandler;
import com.example.societyhub.service.FlatService;
import com.example.societyhub.model.Resident;
import com.example.societyhub.model.Society;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
@SessionAttributes("formData")
public class ResidentController {
    private static final Logger Log = LogManager.getLogger(ResidentController.class);

    private final DBHandler dbHandler;
    private final FlatService flatService;
    private final BillingService billingService;
    private final ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    public ResidentController(DBHandler dbHandler, FlatService flatService,
                               BillingService billingService,
                               ThymeleafViewResolver thymeleafViewResolver) {
        this.dbHandler = dbHandler;
        this.flatService = flatService;
        this.billingService = billingService;
        this.thymeleafViewResolver = thymeleafViewResolver;
    }

    @GetMapping("/resident_dashboard")
    public String getResidentDashboard(Model model, HttpSession session) {

        model.addAttribute("test", "HELLO WORKING");

        String mygate_no = (String) session.getAttribute("residentMygate");
        System.out.println("Session mygate: " + mygate_no);

        if (mygate_no == null) {
            model.addAttribute("error", "Session expired. Please login again.");
            return "error";
        }

        try {
            // 1. Get resident
            Resident resident = dbHandler.getResident(mygate_no);

            if (resident == null) {
                model.addAttribute("error", "Resident not found.");
                return "error";
            }

            model.addAttribute("resident", resident);

            // 2. Get SID from flat (Resident no longer has sid)
            Flat flat = flatService.getFlatByMygate(mygate_no);
            if (flat != null) {
                Integer sid = flat.getSociety_id();
                model.addAttribute("flat", flat);

                // 3. Fetch announcements safely
                List<Announcement> announcements = dbHandler.getAnnouncement(sid);
                model.addAttribute("announcements", announcements);
            }

            return "resident_dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Something went wrong.");
            return "error";
        }
    }

    @GetMapping("/resident_details")
    public String handleResidentData(Model model, HttpSession session, HttpServletRequest request) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            model.addAttribute("error", "Admin society ID not found");
            return "error";
        }

        try {
            Society society = dbHandler.getSocietyBySid(sid);
            model.addAttribute("society_name", society.getName());
            List<Resident> residents = dbHandler.getResident(sid);
            if (residents == null || residents.isEmpty()) {
                model.addAttribute("error", "No resident data available");
                return "error";
            }

            model.addAttribute("residents", residents);

            // Initialize formData for any specific resident update (if needed)
            model.addAttribute("formData", new HashMap<String, String>());
            model.addAttribute("role", "admin");
            model.addAttribute("requestURI", request.getRequestURI());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "admin/resident_details";
    }

    @PostMapping("/add_resident")
    @ResponseBody
    public Map<String, Object> addResident(@RequestBody Map<String, String> formData, HttpSession session) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Map<String, Object> response = new HashMap<>();
        try {
            Resident resident = new Resident();
            resident.setAge(Integer.parseInt(formData.get("age")));
            resident.setContact_no(formData.get("contact_no"));
            resident.setBhk(formData.get("bhk"));
            resident.setEmail(formData.get("email"));
            resident.setIs_admin(false);
            
            boolean isTenant = Boolean.parseBoolean(formData.get("is_tenant"));
            resident.setIs_tenant(isTenant);
            if (isTenant) {
                com.example.societyhub.model.Tenant tenant = new com.example.societyhub.model.Tenant();
                tenant.setName(formData.get("tenant_name"));
                tenant.setContact_no(formData.get("tenant_contact"));
                tenant.setEmail(formData.get("tenant_email"));
                tenant.setBill_type(formData.get("tenant_bill_type"));
                resident.setTenant(tenant);
            }

            // Call service method to add the resident (now requires societyId)
            dbHandler.addResident(resident, sid);

            Log.info("Resident added: Email: " + resident.getEmail());

            response.put("success", true);
            response.put("message", "Resident added successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error adding resident: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/delete_residents")
    @ResponseBody
    public String deleteResidents(@RequestBody Map<String, List<String>> request) {
        List<String> mygateNos = request.get("mygateNos");
        System.out.println("mygateNos: " + mygateNos);
        for (String mygateNo : mygateNos) {
            try {
                System.out.println("mygateNo: " + mygateNo);
                dbHandler.deleteResident(mygateNo);
                Log.info("Resident deleted: Mygate No: " + mygateNo);
            } catch (Exception e) {
                return "Error deleting resident: " + e.getMessage();
            }
        }
        return "success";
    }


    @PostMapping("/update_resident")
    @ResponseBody
    public Map<String, Object> updateResident(@RequestBody Map<String, String> formData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Resident resident = new Resident();
            resident.setAge(Integer.parseInt(formData.get("age")));
            resident.setContact_no(formData.get("contact_no"));
            resident.setBhk(formData.get("bhk"));
            resident.setEmail(formData.get("email"));
            resident.setMygate_no(formData.get("mygate_no"));
            System.out.println("MyGate no: " + resident.getMygate_no());
            
            boolean isTenant = Boolean.parseBoolean(formData.get("is_tenant"));
            resident.setIs_tenant(isTenant);
            if (isTenant) {
                com.example.societyhub.model.Tenant tenant = new com.example.societyhub.model.Tenant();
                tenant.setName(formData.get("tenant_name"));
                tenant.setContact_no(formData.get("tenant_contact"));
                tenant.setEmail(formData.get("tenant_email"));
                tenant.setBill_type(formData.get("tenant_bill_type"));
                resident.setTenant(tenant);
            }

            // Call service method to update the resident data
            dbHandler.updateResident(resident);

            response.put("success", true);
            response.put("message", "Resident updated successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error updating resident: " + e.getMessage());
        }
        return response;
    }

    private int determineFlag(String month, String status) {
        System.out.println("Processing month: " + month + ", status: " + status);

        if ("Paid".equals(status)) {
            return 1;
        } else if ("Unpaid".equals(status)) {
            return 0;
        } else if ("Paid_with_fine".equals(status)) {
            return 2;
        } else {
            return -1; // Invalid status
        }
    }


    @GetMapping("/generateResidentBill")
    public String getResidentBill(@RequestParam(value = "month", required = false) String month,
                                  Model model, HttpSession session, HttpServletRequest request) throws SQLException {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        System.out.println("Sid: " + sid);

        if (sid == null) {
            model.addAttribute("error", "Admin society ID not found");
            return "error";
        }

        try {
            if (month == null || month.isEmpty()) {
                month = "january";
            }
            String monthLower = month.toLowerCase();
            int year = LocalDate.now().getYear();

            String displayMonth = monthLower.substring(0,1).toUpperCase() + monthLower.substring(1);
            model.addAttribute("selectedMonth", displayMonth);

            Society society = dbHandler.getSocietyBySid(sid);
            model.addAttribute("society_name", society.getName());

            List<Resident> residents = dbHandler.getResident(sid);

            if (residents == null || residents.isEmpty()) {
                model.addAttribute("error", "No resident data available");
                return "error";
            }

            // Populate each resident's billing status from unit_bill_record
            for (Resident resident : residents) {
                try {
                    Flat flat = flatService.getFlatByMygate(resident.getMygate_no());
                    if (flat != null) {
                        com.example.societyhub.model.UnitBillRecord ubr =
                                billingService.getUnitBillRecordByFlat(flat.getFlat_id(), monthLower, year);
                        if (ubr != null && ubr.getStatus() != null) {
                            // Convert DB status (PAID/UNPAID/PAID_WITH_FINE/PARTIALLY_PAID) to display format
                            String dbStatus = ubr.getStatus().toUpperCase();
                            if ("PAID".equals(dbStatus))            resident.setStatus("Paid");
                            else if ("PAID_WITH_FINE".equals(dbStatus)) resident.setStatus("Paid_with_fine");
                            else if ("PARTIALLY_PAID".equals(dbStatus)) resident.setStatus("Partially_Paid");
                            else                                         resident.setStatus("Unpaid");
                        } else {
                            resident.setStatus("Unpaid");
                        }
                    }
                } catch (Exception e) {
                    resident.setStatus("Unpaid"); // safe default
                }
            }

            model.addAttribute("residents", residents);
            model.addAttribute("formData", new HashMap<String, String>());
            model.addAttribute("role", "admin");
            model.addAttribute("requestURI", request.getRequestURI());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "admin/generate_bill";
    }

    @PostMapping("/update_status")
    @ResponseBody
    public Map<String, Object> updateResidentStatus(@RequestBody Map<String, String> formData, HttpSession session) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Map<String, Object> response = new HashMap<>();
        try {
            String month = formData.get("month");
            String status = formData.get("status").replace(" ", "_");
            String mygateNo = formData.get("mygate_no");

            System.out.println("Month: " + month);
            System.out.println("Status: " + status);
            System.out.println("MyGate no: " + mygateNo);

            int currentYear = LocalDate.now().getYear();
            
            Flat flat = flatService.getFlatByMygate(mygateNo);
            if (flat != null) {
                com.example.societyhub.model.UnitBillRecord ubr = billingService.getUnitBillRecordByFlat(flat.getFlat_id(), month.toLowerCase(), currentYear);
                if (ubr != null) {
                    if ("Paid".equalsIgnoreCase(status)) {
                        billingService.markAsPaid(ubr.getId());
                    } else if ("Paid_with_fine".equalsIgnoreCase(status)) {
                        billingService.markAsPaidWithFine(ubr.getId(), java.math.BigDecimal.ZERO);
                    } else if ("Partially_Paid".equalsIgnoreCase(status)) {
                        billingService.markAsPartiallyPaid(ubr.getId());
                    } else {
                        billingService.markAsUnpaid(ubr.getId());
                    }
                }
            }

            response.put("success", true);
            response.put("message", "Resident status updated successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error updating resident status: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/resident/complaint")
    public String submitComplaint(@RequestParam String subject,
                                  @RequestParam String description,
                                  HttpSession session,
                                  Model model) {

        String mygateNo = (String) session.getAttribute("residentMygate");

        if (mygateNo == null) {
            model.addAttribute("error", "Session expired. Please login again.");
            return "error";
        }

        try {

            // 1. Fetch resident from DB
            Resident resident = dbHandler.getResident(mygateNo);

            if (resident == null) {
                model.addAttribute("error", "Resident not found.");
                return "error";
            }

            // 2. Get flat info for society_id and flat_no
            Flat flat = flatService.getFlatByMygate(mygateNo);

            // 3. Create complaint
            Complaint complaint = new Complaint();
            complaint.setSocietyId(flat != null ? flat.getSociety_id() : 0);
            complaint.setResidentName(resident.getMem_id()); // Using mem_id as identifier since name is no longer on Resident
            complaint.setFlatNo(flat != null ? flat.getFlat_no() : "N/A");
            complaint.setSubject(subject);
            complaint.setDescription(description);
            complaint.setStatus("PENDING");
            complaint.setCreatedAt(LocalDateTime.now());

            // 4. Save to DB
            dbHandler.saveComplaint(complaint);

            // 5. Reload dashboard data
            model.addAttribute("resident", resident);
            if (flat != null) {
                List<Announcement> announcements =
                        dbHandler.getAnnouncement(flat.getSociety_id());
                model.addAttribute("announcements", announcements);
            }

            model.addAttribute("message", "Complaint submitted successfully.");

            return "resident_dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to submit complaint.");
            return "error";
        }
    }

    @PostMapping("/bulk_update_status")
    @ResponseBody
    public Map<String,String> bulkUpdateStatus(
            @RequestBody Map<String,Object> payload,
            HttpSession session) throws SQLException {

        Integer sid =
                (Integer) session.getAttribute("adminSocietyId");

        String month = (String) payload.get("month");
        String status = (String) payload.get("status");

        List<String> residents =
                (List<String>) payload.get("residents");

        for(String mygateNo : residents){
            int year = LocalDate.now().getYear();
            Flat flat = flatService.getFlatByMygate(mygateNo);
            if (flat != null) {
                com.example.societyhub.model.UnitBillRecord ubr = billingService.getUnitBillRecordByFlat(flat.getFlat_id(), month.toLowerCase(), year);
                if (ubr != null) {
                    if ("Paid".equalsIgnoreCase(status)) {
                        billingService.markAsPaid(ubr.getId());
                    } else if ("Paid_with_fine".equalsIgnoreCase(status)) {
                        billingService.markAsPaidWithFine(ubr.getId(), java.math.BigDecimal.ZERO);
                    } else if ("Partially_Paid".equalsIgnoreCase(status)) {
                        billingService.markAsPartiallyPaid(ubr.getId());
                    } else {
                        billingService.markAsUnpaid(ubr.getId());
                    }
                }
            }
        }

        return Map.of("message","Updated");
    }
}
