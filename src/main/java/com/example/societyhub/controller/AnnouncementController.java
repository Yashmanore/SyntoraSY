package com.example.societyhub.controller;

import com.example.societyhub.service.AnnouncementService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @DeleteMapping("/announcements")
    public String deleteAnnouncement(@RequestBody Map<String, String> request) {
        try {
            announcementService.deleteAnnouncement(
                    Integer.parseInt(request.get("sid")),
                    request.get("title")
            );
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
}

