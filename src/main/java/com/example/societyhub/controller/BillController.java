package com.example.societyhub.controller;

import com.example.societyhub.service.BillingCalculationService;
import com.example.societyhub.service.DBHandler;
import com.example.societyhub.model.Bill;
import com.example.societyhub.model.Society;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class BillController {
    private static final Logger Log = LogManager.getLogger(BillController.class);

    @Autowired
    private final DBHandler dbHandler;
    private final BillingCalculationService billingCalculationService;

    @Autowired
    public BillController(DBHandler dbHandler,
            BillingCalculationService billingCalculationService,
            ThymeleafViewResolver thymeleafViewResolver) {
        this.dbHandler = dbHandler;
        this.billingCalculationService = billingCalculationService;
        this.thymeleafViewResolver = thymeleafViewResolver;
    }

    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    @GetMapping("/form")
    public String showForm() {
        return "redirect:/api/charges";
    }

    public String prepareHtmlForPdf(Integer sid, Map<String, String> formData, Model model, double currentMonthTotal,
            double amountDue) throws Exception {
        // Retrieve society details from the database
        Society society = dbHandler.getSocietyBySid(sid);
        formData.put("society_name", society.getName());
        formData.put("street", society.getStreet());
        formData.put("landmark", society.getLandmark());
        formData.put("locality", society.getLocality());
        formData.put("pincode", society.getPincode());
        formData.put("city", society.getCity());

        formData.put("current_month_total", String.valueOf(currentMonthTotal));
        formData.put("amount_due", String.valueOf(amountDue));
        formData.put("amount_due_in_words", BillingCalculationService.convertNumberToWords((int) amountDue));
        formData.put("fine", String.valueOf(0));

        String billDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        formData.put("bill_date", billDate);

        Integer billNo = dbHandler.getNextBillNumber();
        formData.put("bill_no", billNo.toString());

        // Retrieve resident data from the database
        List<Map<String, String>> residentsData = dbHandler.queryResident(sid);
        if (residentsData == null || residentsData.isEmpty()) {
            model.addAttribute("error", "No resident data found for this session ID");
            return null;
        }

        // Combine form data and resident data to generate bills for all residents
        StringBuilder htmlBuilder = new StringBuilder();
        for (Map<String, String> residentData : residentsData) {
            Map<String, String> billData = new HashMap<>(formData);
            billData.putAll(residentData);

            Context context = new Context();
            context.setVariable("formData", billData);
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
            // TODO: preview-pdf needs reworking for the new bill schema
            double currentMonthTotal = 0.0;
            double amountDue = 0.0;

            String htmlContent = prepareHtmlForPdf(sid, formData, model, currentMonthTotal, amountDue);
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
                Map<String, String> formData = new HashMap<>();
                formData.put("sid", String.valueOf(bill.getSid()));
                formData.put("due_date", String.valueOf(bill.getDue_date()));

                // TODO: generate-pdf needs reworking for new bill schema
                // Individual charge fields are now in bill_line_item + charge_type_history
                double currentMonthTotal = 0.0;
                double amountDue = 0.0;

                String htmlContent = prepareHtmlForPdf(sid, formData, model, currentMonthTotal, amountDue);
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


}
