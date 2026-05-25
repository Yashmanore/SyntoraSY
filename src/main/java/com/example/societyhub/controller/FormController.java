// package com.example.societyhub.controller;

// import com.example.societyhub.service.DBHandler;
// import com.itextpdf.html2pdf.HtmlConverter;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import jakarta.servlet.http.HttpSession;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.ui.Model;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
// import org.springframework.web.bind.support.SessionStatus;
// import org.thymeleaf.context.Context;
// import org.thymeleaf.spring6.view.ThymeleafViewResolver;

// import java.io.ByteArrayInputStream;
// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.io.OutputStream;
// import java.util.Base64;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// @Controller
// @RequestMapping("/api")
// @SessionAttributes("formData")
// public class FormController {
// private static final Logger Log=LogManager.getLogger(FormController.class);

// @GetMapping("/form")
// public String showForm(Model model) {
// if(!model.containsAttribute("formData")){
// // Initialize with empty values if formData does not exist in the session
// Map<String, String> formData = new HashMap<>();
// // formData.put("room_no", "");
// // formData.put("bill_no", "");
// // formData.put("bill_date", "");
// formData.put("maintenance_contribution", "");
// formData.put("sub_charge", "");
// formData.put("housing_board_contribution", "");
// formData.put("fine", "");
// formData.put("property_tax_contribution", "");
// formData.put("building_dev_fund", "");
// formData.put("sinking_fund", "");
// formData.put("other", "");
// formData.put("reserve_mhada_service_charge", "");
// formData.put("bill_for", "");
// formData.put("current_month_total", "");
// formData.put("amount_due_in_words", "");
// formData.put("arrears", "");
// formData.put("bldg_fund_due", "");
// formData.put("amount_due", "");
// model.addAttribute("formData", formData);
// }
// return "bill_form"; // Thymeleaf template for the form
// }

// @PostMapping("/form/submit")
// public String handleSubmit(@RequestParam Map<String, String> formData, Model
// model) {
// // Add form data to the session attributes
// model.addAttribute("formData", formData);

// // Add form data to the model to be displayed in bill.html
// // model.addAttribute("roomNo", formData.get("room_no"));
// // model.addAttribute("billNo", formData.get("bill_no"));
// // model.addAttribute("billDate", formData.get("bill_date"));
// model.addAttribute("maintenanceContribution",
// formData.get("maintenance_contribution"));
// model.addAttribute("sub_charge", formData.get("sub_charge"));
// model.addAttribute("housing_board_contribution",
// formData.get("housing_board_contribution"));
// model.addAttribute("fine", formData.get("fine"));
// model.addAttribute("property_tax_contribution",
// formData.get("property_tax_contribution"));
// model.addAttribute("building_dev_fund", formData.get("building_dev_fund"));
// model.addAttribute("sinking_fund", formData.get("sinking_fund"));
// model.addAttribute("other", formData.get("other"));
// model.addAttribute("reserve_mhada_service_charge",
// formData.get("reserve_mhada_service_charge"));
// model.addAttribute("bill_for", formData.get("bill_for"));
// model.addAttribute("current_month_total",
// formData.get("current_month_total"));
// model.addAttribute("amount_due_in_words",
// formData.get("amount_due_in_words"));
// model.addAttribute("arrears", formData.get("arrears"));
// model.addAttribute("bldg_fund_due", formData.get("bldg_fund_due"));
// model.addAttribute("amount_due", formData.get("amount_due"));

// return "bill"; // Thymeleaf template for the bill
// }

// @PostMapping("/form/modify")
// public String modifyBill(@ModelAttribute("formData") Map<String, String>
// formData, Model model) {
// // Add form data back to the model to pre-fill the form fields
// model.addAllAttributes(formData);
// return "bill_form"; // Return to the form with pre-filled values
// }
// }

// package com.example.societyhub.controller;

// import com.example.societyhub.service.DBHandler;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import jakarta.servlet.http.HttpSession;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
// import com.itextpdf.html2pdf.HtmlConverter;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.*;
// import org.thymeleaf.context.Context;
// import org.thymeleaf.spring6.view.ThymeleafViewResolver;

// import java.io.ByteArrayInputStream;
// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.io.OutputStream;
// import java.util.Base64;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// @Controller
// @RequestMapping("/api")
// @SessionAttributes("formData")
// public class BillController {
// DBHandler dbHandler;
// private static final Logger Log = LogManager.getLogger(BillController.class);

// @Autowired
// private ThymeleafViewResolver thymeleafViewResolver;

// // @GetMapping("/bill")
// // public String showForm(Model model) {
// // if (!model.containsAttribute("formData")) {
// // // Initialize with default empty values
// // Map<String, String> formData = new HashMap<>();
// // formData.put("maintenance_contribution", "");
// // formData.put("amount_44593", "");
// // formData.put("housing_board_contribution", "");
// // formData.put("sub_charge", "");
// // formData.put("property_tax_contribution", "");
// // formData.put("fine", "");
// // formData.put("sinking_fund", "");
// // formData.put("building_dev_fund", "");
// // formData.put("other", "");
// // formData.put("reserve_mhada_service_charge", "");
// // formData.put("current_month_total", "");
// // model.addAttribute("formData", formData);
// // }
// // return "bill_form"; // Thymeleaf template for the form
// // }

// @PostMapping("/bill/modify")
// public String modifyBill(@ModelAttribute("formData") Map<String, String>
// formData, Model model) {
// model.addAllAttributes(formData);
// return "bill_form"; // Return to the form with pre-filled values
// }

// // @PostMapping("/bill/submit")
// // public String handleSubmit(@RequestParam Map<String, String> formData,
// Model model) {
// // model.addAttribute("formData", formData); // Add form data to model
// // return "redirect:/api/preview-pdf";
// // }

// @GetMapping("/preview-pdf")
// public String previewPdf(HttpServletRequest request, Model model) {
// HttpSession session = request.getSession();
// try {
// // Retrieve formData from session attributes
// Map<String, String> formData = (Map<String, String>)
// session.getAttribute("formData");
// if (formData == null) {
// model.addAttribute("error", "No form data available");
// return "error"; // Thymeleaf template for the error page
// }

// // Retrieve resident data from the database
// List<Map<String, String>> residentsData = dbHandler.queryResident(); // Use
// the queryResident method

// // Combine form data and resident data to generate bills for all residents
// StringBuilder htmlBuilder = new StringBuilder();
// for (Map<String, String> residentData : residentsData) {
// Map<String, String> billData = new HashMap<>(formData); // Copy formData into
// billData
// billData.putAll(residentData); // Merge resident data into billData

// // Generate HTML content for each resident's bill using Thymeleaf
// Context context = new Context();
// context.setVariable("formData", billData); // Pass combined data
// String html = thymeleafViewResolver.getTemplateEngine().process("final_bill",
// context); // Process the "final_bill" template
// htmlBuilder.append(html); // Append each bill's HTML
// }

// // Convert the generated HTML into PDF
// String finalHtml = htmlBuilder.toString();
// ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
// HtmlConverter.convertToPdf(new ByteArrayInputStream(finalHtml.getBytes()),
// byteArrayOutputStream);
// byte[] pdfBytes = byteArrayOutputStream.toByteArray();

// // Encode PDF bytes to Base64 and store in session
// String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
// session.setAttribute("pdfBytes", base64Pdf);
// model.addAttribute("pdfBytes", base64Pdf); // Add to model for Thymeleaf view

// return "preview_bill"; // Thymeleaf template for previewing the bill
// } catch (Exception e) {
// e.printStackTrace();
// model.addAttribute("error", "Failed to generate PDF preview");
// return "error"; // Thymeleaf template for the error page
// }
// }

// @GetMapping("/download-pdf")
// public void downloadPdf(HttpServletRequest request, HttpServletResponse
// response) throws IOException {
// HttpSession session = request.getSession();
// try {
// String base64Pdf = (String) session.getAttribute("pdfBytes");
// if (base64Pdf == null) {
// response.sendError(HttpServletResponse.SC_NOT_FOUND, "No PDF available for
// download");
// return;
// }

// // Decode Base64 string to byte array
// byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

// response.setContentType("application/pdf");
// response.setHeader("Content-Disposition", "attachment; filename=bills.pdf");
// response.setContentLength(pdfBytes.length);

// try (OutputStream outputStream = response.getOutputStream()) {
// outputStream.write(pdfBytes);
// }
// } catch (Exception e) {
// e.printStackTrace();
// response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to
// download PDF");
// }
// }

// // Mock method to fetch resident data
// private List<Map<String, String>> fetchResidentsData() {
// // In real implementation, fetch resident data from the database
// // Each map should contain resident-specific information like "name",
// "room_no", etc.
// Map<String, String> resident1 = new HashMap<>();
// resident1.put("name", "John Doe");
// resident1.put("room_no", "101");
// resident1.put("bill_no", "2023001");
// resident1.put("bill_date", "2023-09-01");

// Map<String, String> resident2 = new HashMap<>();
// resident2.put("name", "Jane Smith");
// resident2.put("room_no", "102");
// resident2.put("bill_no", "2023002");
// resident2.put("bill_date", "2023-09-01");

// return List.of(resident1, resident2);
// }
// }
