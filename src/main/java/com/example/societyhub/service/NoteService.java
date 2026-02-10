package com.example.societyhub.service;

import com.example.societyhub.model.Note;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Service
public class NoteService {

    private final DBHandler dbHandler;
    private static final Logger log = LogManager.getLogger(NoteService.class);

    public NoteService(DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public List<Note> getNotes(Integer societyId) {
        try {
            return dbHandler.getNotes(societyId);
        } catch (Exception e) {
            log.error("Error fetching notes", e);
            return List.of();
        }
    }

    public void addNote(String title, String message, Integer sid) throws Exception {
        dbHandler.addNote(title, message, sid);
    }

    public void deleteNote(Integer sid, String title) throws Exception {
        dbHandler.deleteNote(sid, title);
    }
}

