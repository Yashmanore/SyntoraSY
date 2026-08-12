package com.example.societyhub.controller;

import com.example.societyhub.model.Announcement;
import com.example.societyhub.model.Complaint;
import com.example.societyhub.model.UnitBillRecord;
import com.example.societyhub.service.BillingService;
import com.example.societyhub.service.DBHandler;
import com.example.societyhub.service.EmailOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class EmailController {

    private static final Logger log = LogManager.getLogger(EmailController.class);

    private final DBHandler dbHandler;
    private final EmailOrchestrationService orchestrationService;
    private final BillingService billingService;

    public EmailController(DBHandler dbHandler,
                           EmailOrchestrationService orchestrationService,
                           BillingService billingService) {
        this.dbHandler = dbHandler;
        this.orchestrationService = orchestrationService;
        this.billingService = billingService;
    }

    /* ================= VIEW PAGE ================= */

    @GetMapping("/notify_resident")
    public String handleResidentData(Model model,
                                     HttpSession session,
                                     HttpServletRequest request) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            model.addAttribute("error", "Admin society ID not found");
            return "error";
        }

        try {
            List<Announcement> announcements =
                    dbHandler.getAnnouncement(sid);

            List<Complaint> complaints = dbHandler.getComplaintsBySociety(sid);

            model.addAttribute("announcements", announcements);
            model.addAttribute("complaints", complaints);

        } catch (SQLException e) {
            log.error("Error fetching announcements", e);
            model.addAttribute("announcements", List.of());
        }

        model.addAttribute("role", "admin");
        model.addAttribute("requestURI", request.getRequestURI());

        return "admin/notify_resident";
    }

    /* ================= SINGLE RECEIPT ================= */

    @PostMapping("/emailBill")
    public ResponseEntity<Map<String, String>> emailBill(
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {

        Integer sid =
                (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Session ID not found."));
        }

        try {
            String mygateNo = requestBody.get("mygate_no");
            String selectedMonth = requestBody.get("selectedMonth");
            String status = requestBody.get("status");
            String email = requestBody.get("email");

            // Get dynamic charges from billing data
            int year = LocalDate.now().getYear();
            UnitBillRecord ubr = billingService.getUnitBillRecordByMygate(
                    mygateNo, selectedMonth, year);

            if (ubr != null) {
                List<Map<String, Object>> lineItems =
                        billingService.getLineItemsWithDetails(ubr.getId());
                log.info("Fetched {} dynamic line items for mygate={} month={}",
                        lineItems.size(), mygateNo, selectedMonth);
            }

            orchestrationService.sendReceipt(
                    mygateNo,
                    selectedMonth,
                    status,
                    sid,
                    email
            );

            return ResponseEntity.ok(
                    Map.of("message", "Receipt emailed successfully.")
            );

        } catch (Exception e) {
            log.error("Receipt email failed: {}", e.getMessage(), e);
            // Return the real error so we can diagnose on the frontend
            String rootCause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send receipt: " + rootCause));
        }
    }

    /* ================= MONTHLY BILL ================= */

    @PostMapping("/sendBill")
    public String sendMonthlyBills(HttpSession session,
                                   Model model) {

        Integer sid =
                (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            model.addAttribute("error", "Session ID not found.");
            return "admin/notify_resident";
        }

        try {

            orchestrationService.sendMonthlyBills(sid);

            model.addAttribute("message",
                    "Monthly bills sent successfully ✅");

        } catch (Exception e) {
            log.error("Monthly bill sending failed", e);
            model.addAttribute("error",
                    "Failed to send monthly bills.");
        }

        return "admin/notify_resident";
    }

    /* ================= MYGATE ================= */

    @PostMapping("/sendMyGate")
    public String sendMyGate(HttpSession session,
                             Model model) {

        Integer sid =
                (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            model.addAttribute("error", "Session ID not found.");
            return "admin/notify_resident";
        }

        try {

            orchestrationService.sendMyGateNumbers(sid);

            model.addAttribute("message",
                    "MyGate numbers sent successfully ✅");

        } catch (Exception e) {
            log.error("MyGate sending failed", e);
            model.addAttribute("error",
                    "Failed to send MyGate numbers.");
        }

        return "admin/notify_resident";
    }

    /* ================= NOTICE ================= */

    @PostMapping("/sendNotice")
    public String sendNotice(@RequestParam("title") String title,
                             @RequestParam("category") String category,
                             @RequestParam("customMessage") String message,
                             HttpSession session,
                             Model model) {

        Integer sid =
                (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            model.addAttribute("error", "Session ID not found.");
            return "admin/notify_resident";
        }

        try {

            dbHandler.addAnnouncement(title, message, category, sid);

            orchestrationService.sendNotice(message, sid);

            model.addAttribute("message",
                    "Notice sent successfully ✅");

        } catch (Exception e) {
            log.error("Notice sending failed", e);
            model.addAttribute("error",
                    "Failed to send notice.");
        }

        return "admin/notify_resident";
    }

    @PostMapping("/bulk_email_bill")
    @ResponseBody
    public Map<String,String> bulkEmailBill(
            @RequestBody Map<String,Object> payload,
            HttpSession session) throws Exception {

        Integer sid =
                (Integer) session.getAttribute("adminSocietyId");

        String month = (String) payload.get("month");

        List<Map<String,String>> residents =
                (List<Map<String,String>>) payload.get("residents");

        for(Map<String,String> resident : residents){

            String mygateNo = resident.get("mygate_no");
            String status = resident.get("status");

            String email = resident.get("email");

            orchestrationService.sendReceipt(
                    mygateNo,
                    month,
                    status,
                    sid,
                    email
            );
        }

        return Map.of("message","Emails sent");
    }
}
