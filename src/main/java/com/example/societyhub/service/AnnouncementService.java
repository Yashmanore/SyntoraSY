package com.example.societyhub.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {
    private final DBHandler dbHandler;
    private static final Logger log = LogManager.getLogger(AnnouncementService.class);

    public AnnouncementService(DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public void deleteAnnouncement(Integer sid, String title) throws Exception {
        dbHandler.deleteAnnouncement(sid, title);
    }
}
