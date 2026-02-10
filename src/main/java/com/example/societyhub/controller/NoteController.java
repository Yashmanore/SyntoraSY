package com.example.societyhub.controller;

import com.example.societyhub.service.DBHandler;
import com.example.societyhub.service.NoteService;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class NoteController {
    private static final Logger Log = LogManager.getLogger(NoteController.class);

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping("/add_note")
    public Map<String, Object> addNote(@RequestBody Map<String, String> formData,
                                       HttpSession session) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");
        Map<String, Object> response = new HashMap<>();

        try {
            noteService.addNote(formData.get("title"),
                    formData.get("message"),
                    sid);

            response.put("success", true);
            response.put("message", "Note added successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error adding note: " + e.getMessage());
        }

        return response;
    }

    @DeleteMapping("/delete_note")
    public String deleteNote(@RequestBody Map<String, String> request) {
        try {
            noteService.deleteNote(
                    Integer.parseInt(request.get("sid")),
                    request.get("title")
            );
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
}
