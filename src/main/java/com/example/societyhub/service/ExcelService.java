package com.example.societyhub.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;

@Service
public class ExcelService {

    private static final Logger log = LogManager.getLogger(ExcelService.class);

    private final DataSource dataSource;
    private final MyGateService myGateService;

    public ExcelService(DataSource dataSource, MyGateService myGateService) {
        this.dataSource = dataSource;
        this.myGateService = myGateService;
    }

    @Transactional
    public void processExcelFile(File file, int societySid) throws IOException {

        try (Connection conn = dataSource.getConnection();
             FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            conn.setAutoCommit(false);

            validateSociety(conn, societySid);

            String[] adminData = getAdminInfo(conn, societySid);
            String adminName = adminData[0];
            String adminContact = adminData[1];

            int nextMemId = getNextMemId(conn, societySid);

            XSSFSheet sheet = workbook.getSheetAt(0);

            try (PreparedStatement updateAdminStmt =
                         conn.prepareStatement(
                                 "UPDATE resident SET room_no=?, mr_ms=?, gender=?, age=?, mygate_no=?, email=?, bhk=? WHERE name=? AND contact_no=? AND sid=?");

                 PreparedStatement insertResidentStmt =
                         conn.prepareStatement(
                                 "INSERT INTO resident (mem_id, sid, name, room_no, mr_ms, gender, age, contact_no, isadmin, mygate_no, bhk, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?, ?)");

                 PreparedStatement updateSocietyStmt =
                         conn.prepareStatement(
                                 "UPDATE society SET data_uploaded = true WHERE sid = ?")) {

                updateSocietyStmt.setInt(1, societySid);
                updateSocietyStmt.executeUpdate();

                HashSet<String> generatedMyGates = new HashSet<>();

                for (Row row : sheet) {

                    if (row.getRowNum() == 0 || isRowEmpty(row))
                        continue;

                    String name = getCellValueAsString(row.getCell(3));
                    String contactNo = getCellValueAsString(row.getCell(6));

                    boolean isAdmin =
                            adminName.equals(name) &&
                                    adminContact.equals(contactNo);

                    String myGate =
                            myGateService.generateUniqueMyGateNumber(generatedMyGates);

                    if (isAdmin) {

                        updateAdminStmt.setInt(1, getCellValueAsInt(row.getCell(1)));
                        updateAdminStmt.setString(2, getCellValueAsString(row.getCell(2)));
                        updateAdminStmt.setString(3, getCellValueAsString(row.getCell(4)));
                        updateAdminStmt.setInt(4, getCellValueAsInt(row.getCell(5)));
                        updateAdminStmt.setString(5, myGate);
                        updateAdminStmt.setString(6, getCellValueAsString(row.getCell(7)));
                        updateAdminStmt.setString(7, getCellValueAsString(row.getCell(8)));
                        updateAdminStmt.setString(8, name);
                        updateAdminStmt.setString(9, contactNo);
                        updateAdminStmt.setInt(10, societySid);

                        updateAdminStmt.executeUpdate();

                    } else {

                        insertResidentStmt.setInt(1, nextMemId++);
                        insertResidentStmt.setInt(2, societySid);
                        insertResidentStmt.setString(3, name);
                        insertResidentStmt.setInt(4, getCellValueAsInt(row.getCell(1)));
                        insertResidentStmt.setString(5, getCellValueAsString(row.getCell(2)));
                        insertResidentStmt.setString(6, getCellValueAsString(row.getCell(4)));
                        insertResidentStmt.setInt(7, getCellValueAsInt(row.getCell(5)));
                        insertResidentStmt.setString(8, contactNo);
                        insertResidentStmt.setString(9, myGate);
                        insertResidentStmt.setString(10, getCellValueAsString(row.getCell(8)));
                        insertResidentStmt.setString(11, getCellValueAsString(row.getCell(7)));

                        insertResidentStmt.addBatch();
                    }

                    insertResidentBill(conn, myGate);
                }

                insertResidentStmt.executeBatch();
                conn.commit();
            }

        } catch (Exception e) {
            log.error("Excel processing failed", e);
            throw new IOException("Excel processing failed", e);
        }
    }

    /* ================= Helpers ================= */

    private void validateSociety(Connection conn, int sid) throws SQLException {
        try (PreparedStatement stmt =
                     conn.prepareStatement("SELECT COUNT(*) FROM society WHERE sid=?")) {

            stmt.setInt(1, sid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getInt(1) == 0)
                throw new SQLException("Invalid society ID");
        }
    }

    private String[] getAdminInfo(Connection conn, int sid) throws SQLException {
        try (PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT name, contact_no FROM resident WHERE sid=? AND isadmin=true")) {

            stmt.setInt(1, sid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                return new String[]{
                        rs.getString("name"),
                        rs.getString("contact_no")
                };

            throw new SQLException("Admin not found");
        }
    }

    private int getNextMemId(Connection conn, int sid) throws SQLException {
        try (PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT COALESCE(MAX(mem_id),0) FROM resident WHERE sid=?")) {

            stmt.setInt(1, sid);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) + 1;
        }
    }

    private void insertResidentBill(Connection conn, String myGate)
            throws SQLException {

        try (PreparedStatement stmt =
                     conn.prepareStatement(
                             "INSERT INTO resident_bill (mygate_no, year) VALUES (?, ?)")) {

            stmt.setString(1, myGate);
            stmt.setInt(2, LocalDate.now().getYear());
            stmt.executeUpdate();
        }
    }

    private boolean isRowEmpty(Row row) {
        return row.getCell(0) == null ||
                row.getCell(0).getCellType() == CellType.BLANK;
    }

    private int getCellValueAsInt(Cell cell) {
        if (cell == null) return 0;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> Integer.parseInt(cell.getStringCellValue());
            default -> 0;
        };
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> new BigDecimal(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
