package com.example.societyhub.controller;

import com.example.societyhub.model.Admin;
import com.example.societyhub.service.DBHandler;
import com.example.societyhub.model.Society;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/societies")
public class SocietyController {

    @Autowired
    private DBHandler dbHandler;
    Admin admin = new Admin();

    private static final Logger Log = LogManager.getLogger(SocietyController.class);

    @PostMapping("/register")
    public ResponseEntity<String> registerSociety(@RequestBody Society society, HttpSession session) {
        try {
            // Check if the society already exists
            if (dbHandler.societyExists(society.getName())) {
                return ResponseEntity.badRequest().body("Society with this name already exists.");
            }

            int societyId = dbHandler.registerSociety(
                    society.getName(),
                    society.getStreet(),
                    society.getLandmark(),
                    society.getLocality(),
                    society.getCity(),
                    society.getState(),
                    society.getPincode(),
                    society.getCountry()
            );
            if (societyId != -1) {
                System.out.println("Society id: " + societyId);
                admin.setSocietyId(societyId);
                Log.info("Society registered successfully: society id" + societyId);
                session.setAttribute("societyId", societyId); // Save to session
                return ResponseEntity.ok("Society registered successfully with SID: " + societyId);
            } else {
                System.out.println("Failed");
                Log.error("Society registration failed");
                return ResponseEntity.status(500).body("Failed to retrieve the society SID.");
            }
//            return ResponseEntity.ok("Society regsitered successfully");
        } catch (SQLException e) {
            return ResponseEntity.status(500).body("Error registering society: " + e.getMessage());
        }
    }
    
    @GetMapping("/existing")
    public ResponseEntity<List<Society>> getExistingSocieties() {
        try {
            System.out.println("Hi 1");
            List<Society> societies = dbHandler.getAllSocieties();
            System.out.println("Hi 2");
            System.out.println(societies);
            System.out.println("Hi 3");
            return ResponseEntity.ok(societies);
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/setSocietyId")
    public ResponseEntity<String> setSocietyId(@RequestBody Map<String, Integer> requestBody, HttpSession session) {
        Integer societyId = requestBody.get("societyId");
        System.out.println("Existing reg sid: " + societyId);
        if (societyId == null) {
            return ResponseEntity.badRequest().body("Invalid society ID.");
        }
        admin.setSocietyId(societyId);
        session.setAttribute("societyId", societyId);
        System.out.println("Society ID set in session: " + session.getAttribute("societyId"));
        return ResponseEntity.ok("Society ID set in session successfully: " + societyId);
    }
}

