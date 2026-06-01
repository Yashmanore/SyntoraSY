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

            // In the new schema, admin info is determined differently.
            // We still need to identify admin rows for the update-vs-insert logic.
            String[] adminData = getAdminInfo(conn, societySid);
            String adminName = adminData[0];
            String adminContact = adminData[1];

            int nextMemId = getNextMemId(conn, societySid);

            XSSFSheet sheet = workbook.getSheetAt(0);

            // New schema: resident table only has (mem_id, age, contact_no, is_admin, mygate_no, bhk, email, password)
            // Flat-level data (flat_no, society_id, etc.) goes into the flat table.
            try (PreparedStatement updateAdminStmt =
                         conn.prepareStatement(
                                 "UPDATE resident SET age=?, mygate_no=?, email=?, bhk=?, is_tenant=?, tenant_id=?, mr_ms=?, gender=?, room_no=? WHERE contact_no=? AND isadmin=true");

                 PreparedStatement insertResidentStmt =
                         conn.prepareStatement(
                                 "INSERT INTO resident (mem_id, age, contact_no, isadmin, mygate_no, bhk, email, name, sid, is_tenant, tenant_id, mr_ms, gender, room_no) VALUES (?, ?, ?, false, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

                 PreparedStatement insertFlatStmt =
                         conn.prepareStatement(
                                 "INSERT INTO flat (flat_no, society_id, owner_mem_id, occupancy_type, mygate_no) VALUES (?, ?, ?, ?, ?)");

                 PreparedStatement insertTenantStmt =
                         conn.prepareStatement(
                                 "INSERT INTO tenant (name, contact_no) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);

                 PreparedStatement updateSocietyStmt =
                         conn.prepareStatement(
                                 "UPDATE society SET data_uploaded = true WHERE sid = ?")) {

                updateSocietyStmt.setInt(1, societySid);
                updateSocietyStmt.executeUpdate();

                HashSet<String> generatedMyGates = new HashSet<>();

                for (Row row : sheet) {

                    if (row.getRowNum() == 0 || isRowEmpty(row))
                        continue;

                    String name = getCellValueAsString(row.getCell(3)); // Owner Name
                    String contactNo = getCellValueAsString(row.getCell(6)); // Owner Contact
                    int age = getCellValueAsInt(row.getCell(5)); // Owner Age
                    String email = getCellValueAsString(row.getCell(7)); // Owner Email
                    String bhk = getCellValueAsString(row.getCell(8)); // BHK
                    int flatNo = getCellValueAsInt(row.getCell(1)); // Flat No
                    String blockWing = getCellValueAsString(row.getCell(2)); // Block/Wing
                    String mrMs = ""; // Block/Wing replaces Mr/Ms in template, keep empty
                    String gender = getCellValueAsString(row.getCell(4)); // Gender

                    String flatNoStr = blockWing.isEmpty() ? String.valueOf(flatNo) : blockWing + "-" + flatNo;

                    boolean isAdmin =
                            adminName.equals(name) &&
                                    adminContact.equals(contactNo);

                    String myGate =
                            myGateService.generateUniqueMyGateNumber(generatedMyGates);

                    boolean isTenant = false;
                    Cell isTenantCell = row.getCell(9);
                    if (isTenantCell != null) {
                        if (isTenantCell.getCellType() == CellType.BOOLEAN) {
                            isTenant = isTenantCell.getBooleanCellValue();
                        } else if (isTenantCell.getCellType() == CellType.STRING) {
                            String val = isTenantCell.getStringCellValue().trim();
                            isTenant = "yes".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val);
                        }
                    }

                    Integer tenantId = null;
                    if (isTenant) {
                        String tName = getCellValueAsString(row.getCell(10));
                        String tContact = getCellValueAsString(row.getCell(11));
                        
                        insertTenantStmt.setString(1, tName);
                        insertTenantStmt.setString(2, tContact);
                        insertTenantStmt.executeUpdate();
                        try (ResultSet rs = insertTenantStmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                  tenantId = rs.getInt(1);
                            }
                        }
                    }

                    if (isAdmin) {
                        // Update existing admin resident record
                        updateAdminStmt.setInt(1, age);
                        updateAdminStmt.setString(2, myGate);
                        updateAdminStmt.setString(3, email);
                        updateAdminStmt.setString(4, bhk);
                        updateAdminStmt.setBoolean(5, isTenant);
                        if (tenantId != null) {
                            updateAdminStmt.setInt(6, tenantId);
                        } else {
                            updateAdminStmt.setNull(6, java.sql.Types.INTEGER);
                        }
                        updateAdminStmt.setString(7, mrMs);
                        updateAdminStmt.setString(8, gender);
                        updateAdminStmt.setInt(9, flatNo);
                        updateAdminStmt.setString(10, contactNo);

                        updateAdminStmt.executeUpdate();

                    } else {
                        // Insert new resident
                        insertResidentStmt.setInt(1, nextMemId);
                        insertResidentStmt.setInt(2, age);
                        insertResidentStmt.setString(3, contactNo);
                        insertResidentStmt.setString(4, myGate);
                        insertResidentStmt.setString(5, bhk);
                        insertResidentStmt.setString(6, email);
                        insertResidentStmt.setString(7, name);
                        insertResidentStmt.setInt(8, societySid);
                        insertResidentStmt.setBoolean(9, isTenant);
                        if (tenantId != null) {
                            insertResidentStmt.setInt(10, tenantId);
                        } else {
                            insertResidentStmt.setNull(10, java.sql.Types.INTEGER);
                        }
                        insertResidentStmt.setString(11, mrMs);
                        insertResidentStmt.setString(12, gender);
                        insertResidentStmt.setInt(13, flatNo);

                        insertResidentStmt.addBatch();
                    }

                    // Insert flat record for every resident
                    insertFlatStmt.setString(1, flatNoStr);
                    insertFlatStmt.setInt(2, societySid);
                    insertFlatStmt.setString(3, String.valueOf(isAdmin ? getAdminMemId(conn, contactNo) : nextMemId));
                    insertFlatStmt.setString(4, isTenant ? "TENANT" : "OWNER");
                    insertFlatStmt.setString(5, myGate);
                    insertFlatStmt.addBatch();

                    if (!isAdmin) {
                        nextMemId++;
                    }

                    insertResidentBill(conn, myGate);
                }

                insertResidentStmt.executeBatch();
                insertFlatStmt.executeBatch();
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
                             "SELECT contact_no FROM resident WHERE sid=? AND isadmin=true")) {

            stmt.setInt(1, sid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                return new String[]{
                        "", // name is no longer on resident; use empty placeholder
                        rs.getString("contact_no")
                };

            throw new SQLException("Admin not found");
        }
    }

    private String getAdminMemId(Connection conn, String contactNo) throws SQLException {
        try (PreparedStatement stmt =
                     conn.prepareStatement("SELECT mem_id FROM resident WHERE contact_no=? AND isadmin=true")) {
            stmt.setString(1, contactNo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("mem_id");
            return "0";
        }
    }

    private int getNextMemId(Connection conn, int sid) throws SQLException {
        try (PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT COALESCE(MAX(CAST(mem_id AS INTEGER)),0) FROM resident")) {

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
