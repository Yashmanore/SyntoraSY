package com.example.societyhub.controller;

import com.example.societyhub.service.BillingCalculationService;
import com.example.societyhub.service.BillDataAssemblerService;
import com.example.societyhub.service.DBHandler;
import com.example.societyhub.model.Bill;
import com.example.societyhub.model.Society;
import com.example.societyhub.model.Resident;
import com.example.societyhub.model.UnitBillRecord;
import com.example.societyhub.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class BillController {
    private static final Logger Log = LogManager.getLogger(BillController.class);

    @Autowired
    private final DBHandler dbHandler;
    private final BillingCalculationService billingCalculationService;
    private final BillDataAssemblerService billDataAssemblerService;
    private final BillingService billingService;

    @Autowired
    public BillController(DBHandler dbHandler,
            BillingCalculationService billingCalculationService,
            BillDataAssemblerService billDataAssemblerService,
            BillingService billingService,
            ThymeleafViewResolver thymeleafViewResolver) {
        this.dbHandler = dbHandler;
        this.billingCalculationService = billingCalculationService;
        this.billDataAssemblerService = billDataAssemblerService;
        this.billingService = billingService;
        this.thymeleafViewResolver = thymeleafViewResolver;
    }

    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    @GetMapping("/form")
    public String showForm() {
        return "redirect:/api/charges";
    }

    public String prepareHtmlForPdf(Integer sid, String month, String dueDate, Model model) throws Exception {
        // Retrieve resident data from the database
        List<Resident> residents = dbHandler.getResident(sid);
        if (residents == null || residents.isEmpty()) {
            model.addAttribute("error", "No resident data found for this session ID");
            return null;
        }

        // Combine form data and resident data to generate bills for all residents
        StringBuilder htmlBuilder = new StringBuilder();
        for (Resident resident : residents) {
            Map<String, Object> billData = billDataAssemblerService.build(
                    resident.getMygate_no(),
                    month,
                    "UNPAID",
                    sid
            );
            
            if (dueDate != null) {
                billData.put("due_date", dueDate);
            }

            Context context = new Context();
            context.setVariable("formData", billData);
            
            // Expose lineItems as a top-level variable for template iteration
            Object lineItems = billData.get("lineItems");
            if (lineItems != null) {
                context.setVariable("lineItems", lineItems);
            }

            String html = thymeleafViewResolver.getTemplateEngine().process("admin/final_bill", context);
            htmlBuilder.append(html);
        }

        return htmlBuilder.toString();
    }

    public byte[] convertHtmlToPdf(String htmlContent) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8)),
                byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping("/preview-pdf")
    public String previewPdf(HttpServletRequest request, Model model, HttpSession session) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Map<String, String> formData = (Map<String, String>) session.getAttribute("formData");

        if (formData == null) {
            model.addAttribute("error", "Form data missing in session");
            return "error";
        }

        if (sid == null) {
            model.addAttribute("error", "Session ID not found");
            return "error";
        }

        try {
            String month = formData.get("bill_for");
            String dueDate = formData.get("due_date");

            String htmlContent = prepareHtmlForPdf(sid, month, dueDate, model);
            if (htmlContent == null) {
                return "error";
            }

            byte[] pdfBytes = convertHtmlToPdf(htmlContent);
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            session.setAttribute("pdfBytes", base64Pdf);
            model.addAttribute("pdfBytes", base64Pdf);

            return "admin/preview_bill";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to generate PDF preview");
            return "error";
        }
    }



    private int parseIntSafely(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @GetMapping("/generate-pdf")
    public String generatePdf(HttpServletRequest request, Model model, HttpSession session) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        System.out.println("Generate pdf sid: " + sid);

        if (sid == null) {
            model.addAttribute("error", "Session ID (SID) not found");
            return "error";
        }

        try {
            Bill bill = dbHandler.fetchBillDetails(sid);
            if (bill == null) {
                model.addAttribute("error", "No bill data available");
                return "admin/empty_bill";
            }

            else {
                String htmlContent = prepareHtmlForPdf(sid, bill.getMonth(), String.valueOf(bill.getDue_date()), model);
                if (htmlContent == null) {
                    return "error";
                }

                byte[] pdfBytes = convertHtmlToPdf(htmlContent);
                String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
                session.setAttribute("pdfBytes", base64Pdf);
                model.addAttribute("pdfBytes", base64Pdf);

                return "admin/preview_bill";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to generate PDF");
            return "error";
        }
    }

    @GetMapping("/download-pdf")
    public void downloadPdf(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        session = request.getSession();
        try {
            Integer sid = (Integer) session.getAttribute("adminSocietyId");
            String base64Pdf = (String) session.getAttribute("pdfBytes");
            if (base64Pdf == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No PDF available for download");
                return;
            }

            byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=Maintenance_Bills.pdf");
            response.setContentLength(pdfBytes.length);

            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(pdfBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to download PDF");
        }
    }

    @PostMapping("/get_bill_line_items")
    @ResponseBody
    public ResponseEntity<?> getBillLineItems(@RequestBody Map<String, String> requestBody) {
        try {
            String mygateNo = requestBody.get("mygate_no");
            String month = requestBody.get("month").toLowerCase();
            int year = LocalDate.now().getYear();

            UnitBillRecord ubr = billingService.getUnitBillRecordByMygate(mygateNo, month, year);
            if (ubr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No bill record found"));
            }

            List<Map<String, Object>> lineItems = billingService.getLineItemsWithDetails(ubr.getId());
            return ResponseEntity.ok(Map.of("unitBillRecordId", ubr.getId(), "lineItems", lineItems));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/update_bill_line_items")
    @ResponseBody
    public ResponseEntity<?> updateBillLineItems(@RequestBody Map<String, Object> requestBody) {
        try {
            Integer unitBillRecordId = (Integer) requestBody.get("unitBillRecordId");
            List<Map<String, Object>> updates = (List<Map<String, Object>>) requestBody.get("updates");

            for (Map<String, Object> update : updates) {
                Integer id = (Integer) update.get("id");
                BigDecimal amount = new BigDecimal(update.get("amount").toString());
                billingService.updateBillLineItemAmount(id, amount);
            }

            billingService.recalculateUnitBillTotal(unitBillRecordId);
            return ResponseEntity.ok(Map.of("message", "Success"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
