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
    public String showForm(Model model, HttpSession session, HttpServletRequest request) throws SQLException {
//        Integer sid = (Integer) session.getAttribute("sid");
//        String userName = (String) session.getAttribute("userName");
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Integer society_name = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return "error"; // Redirect to an error page or handle accordingly
        }

        // Add session data to the model if needed
//        model.addAttribute("userName", userName);
        model.addAttribute("adminSocietyId", sid);
        if (!model.containsAttribute("formData")) {
            Map<String, String> formData = new HashMap<>();
            // Initialize formData with default empty values
            initializeFormData(sid, formData);
            model.addAttribute("formData", formData);
        }
        model.addAttribute("role", "admin"); // or dynamically fetched
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/bill_form"; // Thymeleaf template for the form
    }

    @PostMapping("/form/submit")
    public String handleSubmit(@RequestParam Map<String, String> formData, Model model, HttpSession session) throws Exception {
//        Integer sid = (Integer) session.getAttribute("sid");
        // Add form data to the session attributes
        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return "error"; // Redirect to an error page or handle accordingly
        }

        Society society = dbHandler.getSocietyBySid(sid);
        System.out.println("Name: " + society.getName());

        // Add session data to the model if needed
//        model.addAttribute("userName", userName);
        model.addAttribute("adminSocietyId", sid);
        model.addAttribute("society_name", society.getName());
        model.addAttribute("formData", formData);

        double maintenanceContribution, subcharge, fine, other, buildingDevFund;
        double housingBoardContribution;
        double propertyTaxContribution;
        double sinkingFund;
        double reserveMhadaServiceCharge;
        double arrears;
        double creditBill;

        double currentMonthTotal;
        double amountDue;

        String due_date;

        // Parse and retrieve form data
        maintenanceContribution = billingCalculationService.parseDoubleSafely(formData.get("maintenance_contribution"));
        housingBoardContribution = billingCalculationService.parseDoubleSafely(formData.get("housing_board_contribution"));
        propertyTaxContribution = billingCalculationService.parseDoubleSafely(formData.get("property_tax_contribution"));
        sinkingFund = billingCalculationService.parseDoubleSafely(formData.get("sinking_fund"));
        reserveMhadaServiceCharge = billingCalculationService.parseDoubleSafely(formData.get("reserve_mhada_service_charge"));
        subcharge = billingCalculationService.parseDoubleSafely(formData.get("sub_charge"));
        other = billingCalculationService.parseDoubleSafely(formData.get("other"));
        buildingDevFund = billingCalculationService.parseDoubleSafely(formData.get("building_dev_fund"));
        fine = billingCalculationService.parseDoubleSafely(formData.get("fine"));
        arrears = billingCalculationService.parseDoubleSafely(formData.get("arrears"));
        creditBill = billingCalculationService.parseDoubleSafely(formData.get("credit_bill"));

        // Perform calculations
        currentMonthTotal = maintenanceContribution + housingBoardContribution + propertyTaxContribution + sinkingFund + reserveMhadaServiceCharge + subcharge + other + buildingDevFund;
        System.out.println("currentMonthTotal: " + currentMonthTotal);
        amountDue = (currentMonthTotal + arrears) - creditBill;
        System.out.println("amount due: " + amountDue);

//        if()

        // Add attributes to the model
        model.addAttribute("maintenanceContribution", maintenanceContribution);
        model.addAttribute("sub_charge", formData.get("sub_charge"));
        model.addAttribute("housing_board_contribution", housingBoardContribution);
        model.addAttribute("fine", 0);
        model.addAttribute("property_tax_contribution", propertyTaxContribution);
        model.addAttribute("building_dev_fund", buildingDevFund);
        model.addAttribute("sinking_fund", sinkingFund);
        model.addAttribute("other", other);
        model.addAttribute("reserve_mhada_service_charge", reserveMhadaServiceCharge);
        model.addAttribute("bill_for", formData.get("bill_for"));
        model.addAttribute("due_date", formData.get("due_date"));
        model.addAttribute("current_month_total", currentMonthTotal);
        model.addAttribute("amount_due_in_words", formData.get("amount_due_in_words"));
        model.addAttribute("arrears", arrears);
        model.addAttribute("bldg_fund_due", formData.get("bldg_fund_due"));
        model.addAttribute("amount_due", amountDue);

        Bill bill = new Bill();
        bill.setSid(sid);
        bill.setMaintenance_contribution((int) maintenanceContribution);
        bill.setHousing_board_contribution((int) housingBoardContribution);
        bill.setProperty_tax_contribution((int) propertyTaxContribution);
        bill.setSinking_fund((int) sinkingFund);
        bill.setReserve_mhada_service_charge((int) reserveMhadaServiceCharge);
        bill.setSub_charge((int) subcharge);
        bill.setFine(0);
        bill.setBuilding_dev_fund((int) buildingDevFund);
        bill.setOther((int) other);
        bill.setDue_date(formData.get("due_date"));

        dbHandler.insertOrUpdateBill(bill);
        session.setAttribute("formData", formData);

        return "admin/bill"; // Thymeleaf template for the bill
    }


    @PostMapping("/bill/modify")
    public String modifyBill(@ModelAttribute("formData") Map<String, String> formData, Model model, HttpSession session) {
//        Integer sid = (Integer) session.getAttribute("sid");
        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return "error"; // Redirect to an error page or handle accordingly
        }

        // Add session data to the model if needed
//        model.addAttribute("userName", userName);
        model.addAttribute("adminSocietyId", sid);
        model.addAllAttributes(formData);
        return "admin/bill_form"; // Return to the form with pre-filled values
    }


    public String prepareHtmlForPdf(Integer sid, Map<String, String> formData, Model model, double currentMonthTotal, double amountDue) throws Exception {
        // Retrieve society details from the database
        Society society = dbHandler.getSocietyBySid(sid);
        formData.put("society_name", society.getName());
        formData.put("street", society.getStreet());
        formData.put("landmark", society.getLandmark());
        formData.put("locality", society.getLocality());
        formData.put("pincode", society.getPincode());
        formData.put("city", society.getCity());

        // Add the calculated values to formData
        formData.put("current_month_total", String.valueOf(currentMonthTotal));
        formData.put("amount_due", String.valueOf(amountDue));
        formData.put("amount_due_in_words", BillingCalculationService.convertNumberToWords((int) amountDue));
        formData.put("fine", String.valueOf(0));


        // Set the bill_date to the current date in India
        String billDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        formData.put("bill_date", billDate);

        // Get the next bill number
        Integer billNo = dbHandler.getNextBillNumber();
        formData.put("bill_no", billNo.toString());

        // Retrieve resident data from the database
        List<Map<String, String>> residentsData = dbHandler.queryResident(sid);
        if (residentsData == null || residentsData.isEmpty()) {
            model.addAttribute("error", "No resident data found for this session ID");
            return null; // Return error if no resident data is found
        }

        // Combine form data and resident data to generate bills for all residents
        StringBuilder htmlBuilder = new StringBuilder();
        for (Map<String, String> residentData : residentsData) {
            Map<String, String> billData = new HashMap<>(formData);
            billData.putAll(residentData);

            // Generate HTML content for each resident's bill
            Context context = new Context();
            context.setVariable("formData", billData);
            String html = thymeleafViewResolver.getTemplateEngine().process("admin/final_bill", context);
            htmlBuilder.append(html);

        }

        return htmlBuilder.toString();
    }

    public byte[] convertHtmlToPdf(String htmlContent) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8)), byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping("/preview-pdf")
    public String previewPdf(HttpServletRequest request, Model model, HttpSession session) {
        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Map<String,String> formData =
                (Map<String,String>) session.getAttribute("formData");

        if (formData == null) {
            model.addAttribute("error", "Form data missing in session");
            return "error";
        }

        if (sid == null) {
            model.addAttribute("error", "Session ID not found");
            return "error"; // Handle missing session ID error
        }

        try {
            saveBillDetails(formData, request, model, session);
            Bill bill = dbHandler.fetchBillDetails(sid);

            double maintenanceContribution = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getMaintenance_contribution()));
            double housingBoardContribution = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getHousing_board_contribution()));
            double propertyTaxContribution = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getProperty_tax_contribution()));
            double sinkingFund = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getSinking_fund()));
            double reserveMhadaServiceCharge = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getReserve_mhada_service_charge()));
            double subcharge = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getSub_charge()));
            double buildingDevFund = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getBuilding_dev_fund()));
            double other = billingCalculationService.parseDoubleSafely(String.valueOf(bill.getOther()));

            double arrears = billingCalculationService.parseDoubleSafely(formData.get("arrears"));
            double creditBill = billingCalculationService.parseDoubleSafely(formData.get("credit_bill"));

            double currentMonthTotal =
                    maintenanceContribution +
                            housingBoardContribution +
                            propertyTaxContribution +
                            sinkingFund +
                            reserveMhadaServiceCharge +
                            subcharge +
                            buildingDevFund +
                            other;

            double amountDue = (currentMonthTotal + arrears) - creditBill;

            // Use the service to prepare and generate the HTML for the bill
            String htmlContent = prepareHtmlForPdf(sid, formData, model, currentMonthTotal, amountDue);
            if (htmlContent == null) {
                return "error"; // Handle error when formData or resident data is missing
            }

            // Convert HTML to PDF
            byte[] pdfBytes = convertHtmlToPdf(htmlContent);

            // Encode PDF bytes to Base64 and add to session
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            session.setAttribute("pdfBytes", base64Pdf);
            model.addAttribute("pdfBytes", base64Pdf);

            return "admin/preview_bill"; // Thymeleaf template for previewing the bill
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to generate PDF preview");
            return "error"; // Thymeleaf template for the error page
        }
    }

    //    @PostMapping("/bill/save")
    public String saveBillDetails(@ModelAttribute("formData") Map<String, String> formData, HttpServletRequest request, Model model, HttpSession session) {
//        session = request.getSession();
        try {
            // Retrieve sid from session
            Integer sid = (Integer) session.getAttribute("adminSocietyId");
            System.out.println("Sid: " + sid);
            if (sid == null) {
                model.addAttribute("error", "No SID available");
                return "error"; // Thymeleaf template for the error page
            }

            Bill bill = new Bill();
            bill.setSid(sid); // Use SID from session
            bill.setMaintenance_contribution(parseIntSafely(formData.get("maintenance_contribution")));
            bill.setHousing_board_contribution(parseIntSafely(formData.get("housing_board_contribution")));
            bill.setProperty_tax_contribution(parseIntSafely(formData.get("property_tax_contribution")));
            bill.setSinking_fund(parseIntSafely(formData.get("sinking_fund")));
            bill.setReserve_mhada_service_charge(parseIntSafely(formData.get("reserve_mhada_service_charge")));
            bill.setSub_charge(parseIntSafely(formData.get("sub_charge")));
            bill.setFine(parseIntSafely(formData.get("fine")));
            bill.setBuilding_dev_fund(parseIntSafely(formData.get("building_dev_fund")));
            bill.setOther(parseIntSafely(formData.get("other")));
            bill.setDue_date(formData.get("due_date"));

            // Save or update bill details to the database
            dbHandler.insertOrUpdateBill(bill);

            model.addAttribute("message", "Bill details saved successfully");
            return "success"; // Thymeleaf template for success page
        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to save bill details");
            return "error"; // Thymeleaf template for the error page
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            return "error"; // Return error page if sid is not available
        }

        try {
            // Fetch Bill details as a Bill object
            Bill bill = dbHandler.fetchBillDetails(sid);
            if (bill == null) {
                model.addAttribute("error", "No bill data available");
                return "admin/empty_bill"; // Return error page if no bill data is available
            }

            else {
                // Convert Bill object to a Map<String, String>
                Map<String, String> formData = new HashMap<>();
                formData.put("sid", String.valueOf(bill.getSid()));
                formData.put("maintenance_contribution", String.valueOf(bill.getMaintenance_contribution()));
                formData.put("housing_board_contribution", String.valueOf(bill.getHousing_board_contribution()));
                formData.put("property_tax_contribution", String.valueOf(bill.getProperty_tax_contribution()));
                formData.put("sinking_fund", String.valueOf(bill.getSinking_fund()));
                formData.put("reserve_mhada_service_charge", String.valueOf(bill.getReserve_mhada_service_charge()));
                formData.put("sub_charge", String.valueOf(bill.getSub_charge()));
                formData.put("fine", String.valueOf(0));
                formData.put("building_dev_fund", String.valueOf(bill.getBuilding_dev_fund()));
                formData.put("other", String.valueOf(bill.getOther()));
                formData.put("due_date", String.valueOf(bill.getDue_date()));
                double maintenanceContribution, subcharge, fine, other, buildingDevFund;
                double housingBoardContribution;
                double propertyTaxContribution;
                double sinkingFund;
                double reserveMhadaServiceCharge;
                double creditBill;

                double currentMonthTotal;

                String due_date;

                // Retrieve values from formData and convert to double for calculations
                maintenanceContribution = billingCalculationService.parseDoubleSafely(formData.get("maintenance_contribution"));
                housingBoardContribution = billingCalculationService.parseDoubleSafely(formData.get("housing_board_contribution"));
                propertyTaxContribution = billingCalculationService.parseDoubleSafely(formData.get("property_tax_contribution"));
                sinkingFund = billingCalculationService.parseDoubleSafely(formData.get("sinking_fund"));
                reserveMhadaServiceCharge = billingCalculationService.parseDoubleSafely(formData.get("reserve_mhada_service_charge"));
                subcharge = billingCalculationService.parseDoubleSafely(formData.get("sub_charge"));
                fine = billingCalculationService.parseDoubleSafely(formData.get("fine"));
                buildingDevFund = billingCalculationService.parseDoubleSafely(formData.get("building_dev_fund"));
                other = billingCalculationService.parseDoubleSafely(formData.get("other"));

                // Assuming arrears value is known or retrieved from somewhere else
                double arrears = 0.0; // Set arrears value

                // Perform calculations for currentMonthTotal and amountDue
                currentMonthTotal = maintenanceContribution + housingBoardContribution +
                        propertyTaxContribution + sinkingFund +
                        reserveMhadaServiceCharge + subcharge +
                        buildingDevFund + other;

                double amountDue = currentMonthTotal - arrears;

                // Output results
                System.out.println("Current Month Total: " + currentMonthTotal);
                System.out.println("Amount Due: " + amountDue);
                // Use the service to generate HTML for PDF
                String htmlContent = prepareHtmlForPdf(sid, formData, model, currentMonthTotal, amountDue);
                System.out.println(currentMonthTotal + " " + amountDue);
//            System.out.println(htmlContent);
                if (htmlContent == null) {
                    return "error"; // Return error page if resident data is missing
                }

                // Convert the generated HTML into PDF
                byte[] pdfBytes = convertHtmlToPdf(htmlContent);

                // Store PDF in session or return it as needed (assuming you want to preview it)
                String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
                session.setAttribute("pdfBytes", base64Pdf);
                model.addAttribute("pdfBytes", base64Pdf);

                return "admin/preview_bill"; // Thymeleaf template for previewing the bill
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to generate PDF");
            return "error"; // Return error page on failure
        }
    }

    @GetMapping("/download-pdf")
    public void downloadPdf(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        session = request.getSession();
        try {
            Integer sid = (Integer) session.getAttribute("adminSocietyId");
            String base64Pdf = (String) session.getAttribute("pdfBytes");
            if (base64Pdf == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No PDF available for download");
                return;
            }

            // Decode Base64 string to byte array
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

    private void initializeFormData(int sid, Map<String, String> formData) throws SQLException {
        try {
            // Fetch Bill details as a Bill object
            Bill bill = dbHandler.fetchBillDetails(sid);
            if (bill == null) {
                formData.put("sid", ""); // Add this for the ID field
                formData.put("maintenance_contribution", "");
                formData.put("sub_charge", "");
                formData.put("housing_board_contribution", "");
                formData.put("fine", "");
                formData.put("property_tax_contribution", "");
                formData.put("building_dev_fund", "");
                formData.put("sinking_fund", "");
                formData.put("other", "");
                formData.put("reserve_mhada_service_charge", "");
                formData.put("bill_for", "");
                formData.put("due_date", "");
                formData.put("current_month_total", "");
                formData.put("amount_due_in_words", "");
                formData.put("arrears", "");
                formData.put("bldg_fund_due", "");
                formData.put("amount_due", "");
                formData.put("credit_bill", "");
            } else {
                // Convert Bill object to a Map<String, String>
//            Map<String, String> formData = new HashMap<>();
                formData.put("sid", String.valueOf(bill.getSid()));
                formData.put("maintenance_contribution", String.valueOf(bill.getMaintenance_contribution()));
                formData.put("housing_board_contribution", String.valueOf(bill.getHousing_board_contribution()));
                formData.put("property_tax_contribution", String.valueOf(bill.getProperty_tax_contribution()));
                formData.put("sinking_fund", String.valueOf(bill.getSinking_fund()));
                formData.put("reserve_mhada_service_charge", String.valueOf(bill.getReserve_mhada_service_charge()));
                formData.put("sub_charge", String.valueOf(bill.getSub_charge()));
                formData.put("fine", String.valueOf(bill.getFine()));
                formData.put("building_dev_fund", String.valueOf(bill.getBuilding_dev_fund()));
                formData.put("other", String.valueOf(bill.getOther()));
                formData.put("bill_for", "");
                formData.put("due_date", "");
                formData.put("credit_bill", "");

                double maintenanceContribution, subcharge, fine, other, buildingDevFund;
                double housingBoardContribution;
                double propertyTaxContribution;
                double sinkingFund;
                double reserveMhadaServiceCharge;
                double creditBill;

                double currentMonthTotal;

                String due_date;

                // Retrieve values from formData and convert to double for calculations
                maintenanceContribution = billingCalculationService.parseDoubleSafely(formData.get("maintenance_contribution"));
                housingBoardContribution = billingCalculationService.parseDoubleSafely(formData.get("housing_board_contribution"));
                propertyTaxContribution = billingCalculationService.parseDoubleSafely(formData.get("property_tax_contribution"));
                sinkingFund = billingCalculationService.parseDoubleSafely(formData.get("sinking_fund"));
                reserveMhadaServiceCharge = billingCalculationService.parseDoubleSafely(formData.get("reserve_mhada_service_charge"));
                subcharge = billingCalculationService.parseDoubleSafely(formData.get("sub_charge"));
                fine = billingCalculationService.parseDoubleSafely(formData.get("fine"));
                buildingDevFund = billingCalculationService.parseDoubleSafely(formData.get("building_dev_fund"));
                other = billingCalculationService.parseDoubleSafely(formData.get("other"));

                // Assuming arrears value is known or retrieved from somewhere else
                double arrears = 0.0; // Set arrears value

                formData.put("amount_due_in_words", "");
                formData.put("arrears", String.valueOf(arrears));
                formData.put("bldg_fund_due", "");

                // Perform calculations for currentMonthTotal and amountDue
                currentMonthTotal = maintenanceContribution + housingBoardContribution +
                        propertyTaxContribution + sinkingFund +
                        reserveMhadaServiceCharge + subcharge +
                        buildingDevFund + other;

                formData.put("current_month_total", String.valueOf(currentMonthTotal));

                double amountDue = currentMonthTotal - arrears;
                formData.put("amount_due", String.valueOf(amountDue));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}

