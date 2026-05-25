package com.example.societyhub.controller;

import com.example.societyhub.model.Admin;
import com.example.societyhub.model.Resident;
import com.example.societyhub.model.WebAdmin;
import com.example.societyhub.service.*;
import com.github.javaparser.utils.Log;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LogManager.getLogger(AuthController.class);

    private final DBHandler dbHandler;
    private final MailService mailService;
    private final PasswordService passwordService;
    private final MyGateService myGateService;

    public AuthController(DBHandler dbHandler,
                          EmailOrchestrationService emailService, MailService mailService,
                          PasswordService passwordService, MyGateService myGateService) {
        this.dbHandler = dbHandler;
        this.mailService = mailService;
        this.passwordService = passwordService;
        this.myGateService = myGateService;
    }

    // ================= REGISTER ADMIN =================

    @PostMapping("/register")
    public ResponseEntity<String> registerAdmin(@RequestBody Admin admin, HttpSession session) {

        Integer societyId = (Integer) session.getAttribute("societyId");
        if (societyId == null) {
            return ResponseEntity.badRequest().body("Society ID not found in session.");
        }

        try {
            if (dbHandler.adminExists(admin.getEmail_id())) {
                return ResponseEntity.badRequest().body("Admin already exists.");
            }

            String hashedPassword = passwordService.encode(admin.getAdminPassword());

            dbHandler.registerAdmin(admin.getEmail_id(), hashedPassword);
            dbHandler.update(societyId,
                    admin.getName(),
                    admin.getContact_no(),
                    admin.getEmail_id());

            log.info("Admin registered successfully: {}", admin.getEmail_id());
            return ResponseEntity.ok("Admin registered successfully.");

        } catch (Exception e) {
            log.error("Admin registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed.");
        }
    }

    // ================= RESIDENT LOGIN =================

    @PostMapping("/residentLogin")
    public ResponseEntity<Map<String, Object>> residentLogin(@RequestBody Resident resident,
                                                             HttpSession session) {

        try {
            String storedHash = dbHandler.getPasswordByMyGateNo(resident.getMygate_no());
            if (storedHash == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "User does not exist"));
            }

            if (!passwordService.matches(resident.getPassword(), storedHash)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials"));
            }

            Resident fullResident = dbHandler.getResident(resident.getMygate_no());

            session.setAttribute("residentMygate", resident.getMygate_no());

            log.info("Resident login success: {}", resident.getMygate_no());

            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (Exception e) {
            log.error("Resident login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error"));
        }
    }

    // ================= ADMIN LOGIN =================

    @PostMapping("/adminLogin")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Admin admin,
                                                          HttpSession session) {

        try {
            String storedHash = dbHandler.getPasswordByEmail(admin.getEmail_id());
            if (storedHash == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "User does not exist"));
            }

            if (!passwordService.matches(admin.getAdminPassword(), storedHash)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials"));
            }

            Admin completeAdmin = dbHandler.getAdminDetails(admin.getEmail_id());
            Boolean dataUploaded = dbHandler.isDataUploaded(completeAdmin.getSocietyId());

            session.setAttribute("adminEmail", completeAdmin.getEmail_id());
            session.setAttribute("adminName", completeAdmin.getName());
            session.setAttribute("adminSocietyId", completeAdmin.getSocietyId());
            session.setAttribute("adminMemId", completeAdmin.getMem_id());
            session.setAttribute("dataUploaded", dataUploaded);

            log.info("Admin login success: {}", admin.getEmail_id());

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "dataUploaded", dataUploaded
            ));

        } catch (Exception e) {
            log.error("Admin login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error"));
        }
    }

    // ================= WEB ADMIN LOGIN =================

    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> webAdminLogin(@RequestBody WebAdmin webAdmin,
                                                             HttpSession session) {

        try {
            String storedHash = dbHandler.getAdminPassword(webAdmin.getUsername());
            if (storedHash == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "User does not exist"));
            }

            if (!passwordService.matches(webAdmin.getPassword(), storedHash)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials"));
            }

            session.setAttribute("webAdminUsername", webAdmin.getUsername());

            log.info("WebAdmin login success: {}", webAdmin.getUsername());

            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (Exception e) {
            log.error("WebAdmin login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error"));
        }
    }

    // ================= LOGOUT =================

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    // ================= SEND OTP =================

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody Map<String, String> request,
                                          HttpSession session) {

        String mygateNo = request.get("mygate_no");

        try {
            Resident resident = dbHandler.getResident(mygateNo);
            if (resident == null) {
                return ResponseEntity.badRequest().body("Resident not found");
            }

            String otp = String.format("%04d", new Random().nextInt(10000));

            session.setAttribute("otp", otp);
            session.setAttribute("otpMygate", mygateNo);

            mailService.sendEmail(
                    resident.getEmail(),
                    "MyGate Authentication OTP",
                    "Your OTP is: " + otp,
                    null,
                    resident.getMem_id()
            );

            log.info("OTP sent for {}", mygateNo);

            return ResponseEntity.ok("OTP sent successfully");

        } catch (Exception e) {
            log.error("OTP send error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending OTP");
        }
    }

    // ================= VALIDATE OTP =================

    @PostMapping("/validate-otp")
    public ResponseEntity<Map<String, Object>> validateOtp(@RequestBody Map<String, String> request,
                                                           HttpSession session) {

        String inputOtp = request.get("otp");
        String sessionOtp = (String) session.getAttribute("otp");

        if (sessionOtp != null && sessionOtp.equals(inputOtp)) {
            return ResponseEntity.ok(Map.of("status", "ok", "message", "OTP validated."));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", "Invalid OTP."));
    }

    @PostMapping("/validate-mygate_no")
    public ResponseEntity<Map<String, Object>> validateMyGate(@RequestBody Map<String, String> request) {
        String mygate_no = request.get("mygate_no");
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("Received MyGate number: " + mygate_no);

            // Validate MyGate number from the database
            boolean residentExists = myGateService.existsInDatabase(mygate_no);
            System.out.println("Does MyGate number exist in database? " + residentExists);

            if (residentExists) {
                // If valid, respond with a success message
                response.put("status", "ok");
                response.put("message", "MyGate number validated.");
                return ResponseEntity.ok(response);
            } else {
                // MyGate number not found
                response.put("status", "error");
                response.put("message", "Invalid MyGate number.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An error occurred during validation.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/create-password")
    public ResponseEntity<Map<String, Object>> createPassword(@RequestBody Map<String, String> request, HttpSession session) {
        System.out.println("createPassword method called");
        Map<String, Object> response = new HashMap<>();

        String mygate_no = request.get("mygate_no");
        System.out.println("mygate:" + mygate_no);
        String password = request.get("password");
        System.out.println("Password:" + password);
        String comPassword = request.get("comPassword"); // Change from "confirmPassword" to "comPassword"

        System.out.println("Password is been created: " + comPassword);

        try {
            // Ensure the passwords match
            if (!password.equals(comPassword)) {
                response.put("status", "error");
                response.put("message", "Passwords do not match.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Hash the password
            String hashedPassword = passwordService.encode(password);
            System.out.println("Hashed password: " + hashedPassword);

            // Update password in the database
            boolean updateSuccess = dbHandler.updateResidentPassword(mygate_no, hashedPassword);

            if (updateSuccess) {
                response.put("status", "ok");
                response.put("message", "Password created successfully.");
                session.setAttribute("residentMygate", mygate_no);
                Log.info("Password set successfully for MyGate No: " + mygate_no);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to update password. Resident not found.");
                Log.error("Failed to create password for MyGate No: " + mygate_no);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An error occurred while creating password.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
