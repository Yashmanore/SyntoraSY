package com.example.societyhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Random;

@Service
public class MyGateService {

    private final DataSource dataSource;

    public MyGateService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String generateUniqueMyGateNumber(
            HashSet<String> sessionNumbers
    ) throws Exception {

        Random random = new Random();
        String number;

        try (Connection conn = dataSource.getConnection()) {

            do {
                number = String.format("%06d",
                        random.nextInt(999999));
            }
            while (
                    sessionNumbers.contains(number) ||
                            existsInDatabase(conn, number)
            );
        }

        sessionNumbers.add(number);
        return number;
    }


    private boolean existsInDatabase(Connection conn, String number)
            throws Exception {

        try (PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT COUNT(*) FROM resident WHERE mygate_no=?")) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public boolean existsInDatabase(String number) throws Exception {

        if (number == null) return false;

        number = number.trim();

        try (Connection conn = dataSource.getConnection()) {
            return existsInDatabase(conn, number);
        }
    }

}
