package com.example.societyhub.controller;

import com.example.societyhub.model.Admin;
import com.example.societyhub.model.Resident;
import com.example.societyhub.model.WebAdmin;
import com.example.societyhub.service.DBHandler;
import com.example.societyhub.service.EmailOrchestrationService;
import com.example.societyhub.service.MailService;
import com.example.societyhub.service.PasswordService;
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

    public AuthController(DBHandler dbHandler,
                          EmailOrchestrationService emailService, MailService mailService,
                          PasswordService passwordService) {
        this.dbHandler = dbHandler;
        this.mailService = mailService;
        this.passwordService = passwordService;
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

            if (!passwordService.matches(resident.getResidentPassword(), storedHash)) {
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
                    resident.getName()
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
}
