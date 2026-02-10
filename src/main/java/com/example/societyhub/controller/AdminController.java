package com.example.societyhub.controller;

import com.example.societyhub.service.DBHandler;
import com.example.societyhub.model.Note;
import com.example.societyhub.service.NoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private static final Logger log = LogManager.getLogger(AdminController.class);

    private final NoteService noteService;

    public AdminController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/admin")
    public String getAdminPage(Model model, HttpSession session, HttpServletRequest request) {

        Integer societyId = (Integer) session.getAttribute("adminSocietyId");
        String adminName = (String) session.getAttribute("adminName");

        if (societyId == null) {
            log.warn("Session expired or invalid session while accessing /admin");
            return "redirect:/login";
        }

        log.info("Admin dashboard requested for societyId={}", societyId);

        List<Note> notes = noteService.getNotes(societyId);

        model.addAttribute("notes", notes);
        model.addAttribute("societyId", societyId);
        model.addAttribute("adminName", adminName);
        model.addAttribute("newNote", new Note());
        model.addAttribute("role", "admin");
        model.addAttribute("requestURI", request.getRequestURI());

        return "admin/admin";
    }

    @GetMapping("/upload")
    public String getUploadPage(Model model, HttpSession session) {

        Integer societyId = (Integer) session.getAttribute("adminSocietyId");
        String adminName = (String) session.getAttribute("adminName");

        if (societyId == null) {
            log.warn("Session expired while accessing /upload");
            return "redirect:/login";
        }

        model.addAttribute("societyId", societyId);
        model.addAttribute("adminName", adminName);

        return "upload";
    }
}
