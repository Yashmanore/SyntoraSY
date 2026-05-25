package com.example.societyhub.service;

import com.example.societyhub.model.Society;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class WebAdminService {

    private final DBHandler dbHandler;

    public WebAdminService(DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public List<Society> getSocietyDashboardData() throws SQLException {
        // Society no longer has residents/bills/admins list fields.
        // Return basic society data; related data should be fetched via
        // separate service calls when needed.
        return dbHandler.getAllSocieties();
    }
}
