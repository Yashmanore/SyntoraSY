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

        List<Society> societies = dbHandler.getAllSocieties();

        for (Society society : societies) {
            society.setResidents(dbHandler.getResident(society.getSid()));
            society.setBills(dbHandler.fetchBillDetails(society.getSid()));
            society.setAdmins(dbHandler.getAdmin(society.getSid()));
        }

        return societies;
    }
}

