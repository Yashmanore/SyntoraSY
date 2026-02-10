package com.example.societyhub.controller;

import com.example.societyhub.service.DBHandler;
import com.example.societyhub.model.Society;
import com.example.societyhub.service.WebAdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.SQLException;
import java.util.List;

@Controller
public class WebAdminController {
    @Autowired
    WebAdminService webAdminService;

    @GetMapping("/web_admin")
    public String getAdminPage(Model model, HttpSession session) throws SQLException {

        String username = (String) session.getAttribute("webAdminUsername");

        model.addAttribute("webAdminUsername", username);
        model.addAttribute("societies", webAdminService.getSocietyDashboardData());

        return "webadmin/web_admin";
    }

}