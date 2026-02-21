package com.example.societyhub.controller;

import com.example.societyhub.model.Complaint;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.societyhub.service.DBHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/complaints")
public class ComplaintController {

    private final DBHandler dbHandler;

    @Autowired
    public ComplaintController(DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }


    /* =========================
       Mark Complaint as Resolved
       ========================= */
    @PostMapping("/resolve")
    public String resolveComplaint(@RequestParam Long id,
                                   HttpSession session) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return "redirect:/login";
        }

        dbHandler.markComplaintResolved(id);

        return "redirect:/api/notify_resident";
    }

    /* =========================
       Delete Complaint
       ========================= */
    @DeleteMapping("/delete")
    public String deleteComplaint(@RequestParam Long id,
                                  HttpSession session) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return "redirect:/login";
        }

        dbHandler.deleteComplaint(id);

        return "redirect:/api/notify_resident";
    }
}
