package com.example.societyhub.service;

import com.example.societyhub.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core billing engine that generates occupancy-aware monthly bills.
 *
 * Flow:
 *  1. Admin triggers bill generation for a society + month/year + due_date
 *  2. A single Bill record is created (society-wide billing cycle)
 *  3. For each flat in the society:
 *       a. Read occupancy_type (OWNER / TENANT)
 *       b. Filter applicable ChargeTypes
 *       c. Snapshot each applicable charge → ChargeTypeHistory
 *       d. Create a UnitBillRecord (flat-level bill)
 *       e. Create BillLineItems linking UnitBillRecord to each ChargeTypeHistory
 *       f. Cache the total on UnitBillRecord
 */
@Service
public class BillingService {

    private final DataSource dataSource;
    private final FlatService flatService;
    private final ChargeTypeService chargeTypeService;

    @Autowired
    public BillingService(DataSource dataSource,
                          FlatService flatService,
                          ChargeTypeService chargeTypeService) {
        this.dataSource = dataSource;
        this.flatService = flatService;
        this.chargeTypeService = chargeTypeService;
    }

    // ─── BILL GENERATION ────────────────────────────────────────────────────────

    /**
     * Generate monthly bills for an entire society.
     * This is the main entry point for the billing cycle.
     *
     * @param societyId the society to bill
     * @param month     billing month (e.g. "may")
     * @param year      billing year (e.g. 2026)
     * @param dueDate   due date string (e.g. "2026-05-15")
     * @return the generated Bill id
     */
    public int generateMonthlyBills(int societyId, String month, int year, String dueDate) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 0. Delete existing bills for this month/year to allow regeneration
            deleteExistingBills(conn, societyId, month, year);

            // 1. Create the society-wide Bill record
            int billId = createBill(conn, societyId, month, year, dueDate);

            // 2. Get all flats in the society
            List<Flat> flats = flatService.getFlatsBySociety(societyId);

            // 3. Get all active charge types for this society
            List<ChargeType> allChargeTypes = chargeTypeService.getChargeTypesBySociety(societyId);

            // 4. For each flat, generate occupancy-aware unit bills
            for (Flat flat : flats) {
                String occupancyType = flat.getOccupancy_type() != null
                        ? flat.getOccupancy_type().toUpperCase()
                        : "OWNER";

                // Filter charges applicable to this flat's occupancy type
                List<ChargeType> applicableCharges = filterChargesByOccupancy(allChargeTypes, occupancyType);

                if (applicableCharges.isEmpty()) {
                    continue; // no charges for this flat, skip
                }

                // Create history snapshots and collect IDs
                List<Integer> historyIds = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                for (ChargeType charge : applicableCharges) {
                    int historyId = chargeTypeService.createChargeTypeHistorySnapshot(
                            conn, charge, societyId, month, year);
                    historyIds.add(historyId);
                    totalAmount = totalAmount.add(charge.getDefault_amount());
                }

                // Create UnitBillRecord for this flat
                int unitBillRecordId = createUnitBillRecord(
                        conn, billId, flat.getFlat_id(), "UNPAID", totalAmount, month, year);

                // Create BillLineItems linking the UnitBillRecord to each ChargeTypeHistory
                for (int i = 0; i < historyIds.size(); i++) {
                    createBillLineItem(conn, unitBillRecordId, historyIds.get(i),
                            applicableCharges.get(i).getDefault_amount());
                }
            }

            conn.commit();
            return billId;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new SQLException("Failed to generate monthly bills", e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // ─── UNIT BILL RECORD QUERIES ───────────────────────────────────────────────

    /**
     * Get all UnitBillRecords for a society's billing cycle (month + year).
     */
    public List<UnitBillRecord> getUnitBillRecordsBySociety(int societyId, String month, int year) throws SQLException {
        List<UnitBillRecord> records = new ArrayList<>();
        String query = "SELECT ubr.id, ubr.bill_id, ubr.flat_id, ubr.status, ubr.total_amount, " +
                "ubr.fine_amount, ubr.paid_date, ubr.month, ubr.year " +
                "FROM unit_bill_record ubr " +
                "JOIN bill b ON ubr.bill_id = b.id " +
                "WHERE b.sid = ? AND ubr.month = ? AND ubr.year = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societyId);
            ps.setString(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapUnitBillRecord(rs));
                }
            }
        }
        return records;
    }

    /**
     * Get the UnitBillRecord for a specific flat and billing cycle.
     */
    public UnitBillRecord getUnitBillRecordByFlat(int flatId, String month, int year) throws SQLException {
        String query = "SELECT id, bill_id, flat_id, status, total_amount, fine_amount, " +
                "paid_date, month, year " +
                "FROM unit_bill_record WHERE flat_id = ? AND month = ? AND year = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, flatId);
            ps.setString(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUnitBillRecord(rs);
                }
            }
        }
        return null;
    }

    /**
     * Get all line items (charges breakdown) for a specific UnitBillRecord.
     * Returns each charge name, amount, and applicability.
     */
    public List<Map<String, Object>> getLineItemsForUnitBill(int unitBillRecordId) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        String query = "SELECT bli.id, bli.amount, cth.name_at_billing, cth.applicable_to " +
                "FROM bill_line_item bli " +
                "JOIN charge_type_history cth ON bli.charge_type_history_id = cth.history_id " +
                "WHERE bli.unit_bill_record_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, unitBillRecordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("charge_name", rs.getString("name_at_billing"));
                    item.put("amount", rs.getBigDecimal("amount"));
                    item.put("applicable_to", rs.getString("applicable_to"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * Get line items WITH charge type history details for a specific UnitBillRecord.
     * Returns each charge's id, name (as "name"), amount, and applicable_to.
     */
    public List<Map<String, Object>> getLineItemsWithDetails(int unitBillRecordId) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT bli.id, bli.amount, cth.name_at_billing, cth.applicable_to " +
                     "FROM bill_line_item bli " +
                     "JOIN charge_type_history cth ON bli.charge_type_history_id = cth.history_id " +
                     "WHERE bli.unit_bill_record_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitBillRecordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("name", rs.getString("name_at_billing"));
                    item.put("amount", rs.getBigDecimal("amount"));
                    item.put("applicable_to", rs.getString("applicable_to"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * Get UnitBillRecord by mygate_no, month, and year.
     * Joins with flat table to resolve mygate_no → flat_id.
     */
    public UnitBillRecord getUnitBillRecordByMygate(String mygateNo, String month, int year) throws SQLException {
        String sql = "SELECT ubr.id, ubr.bill_id, ubr.flat_id, ubr.status, ubr.total_amount, ubr.fine_amount, ubr.paid_date, ubr.month, ubr.year " +
                     "FROM unit_bill_record ubr " +
                     "JOIN flat f ON ubr.flat_id = f.flat_id " +
                     "WHERE f.mygate_no = ? AND ubr.month = ? AND ubr.year = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mygateNo);
            ps.setString(2, month.toLowerCase());
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UnitBillRecord ubr = new UnitBillRecord();
                    ubr.setId(rs.getInt("id"));
                    ubr.setBill_id(rs.getInt("bill_id"));
                    ubr.setFlat_id(rs.getInt("flat_id"));
                    ubr.setStatus(rs.getString("status"));
                    ubr.setTotal_amount(rs.getBigDecimal("total_amount"));
                    ubr.setFine_amount(rs.getBigDecimal("fine_amount"));
                    java.sql.Date pd = rs.getDate("paid_date");
                    ubr.setPaid_date(pd != null ? pd.toLocalDate() : null);
                    ubr.setMonth(rs.getString("month"));
                    ubr.setYear(rs.getInt("year"));
                    return ubr;
                }
            }
        }
        return null;
    }

    // ─── PAYMENT STATUS UPDATES ─────────────────────────────────────────────────

    /**
     * Mark a flat's bill as PAID for a given month/year.
     */
    public void markAsPaid(int unitBillRecordId) throws SQLException {
        String query = "UPDATE unit_bill_record SET status = 'PAID', paid_date = CURRENT_DATE WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setInt(1, unitBillRecordId);
            ps.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Mark a flat's bill as PAID_WITH_FINE, applying a fine amount.
     */
    public void markAsPaidWithFine(int unitBillRecordId, BigDecimal fineAmount) throws SQLException {
        String query = "UPDATE unit_bill_record SET status = 'PAID_WITH_FINE', " +
                "fine_amount = ?, paid_date = CURRENT_DATE WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setBigDecimal(1, fineAmount);
            ps.setInt(2, unitBillRecordId);
            ps.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Reset a flat's bill back to UNPAID.
     */
    public void markAsUnpaid(int unitBillRecordId) throws SQLException {
        String query = "UPDATE unit_bill_record SET status = 'UNPAID', fine_amount = NULL, " +
                "paid_date = NULL WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setInt(1, unitBillRecordId);
            ps.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Update the amount of a specific bill line item.
     */
    public void updateBillLineItemAmount(int lineItemId, BigDecimal newAmount) throws SQLException {
        String query = "UPDATE bill_line_item SET amount = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBigDecimal(1, newAmount);
            ps.setInt(2, lineItemId);
            ps.executeUpdate();
        }
    }

    /**
     * Recalculate and update the total_amount for a unit bill record.
     */
    public void recalculateUnitBillTotal(int unitBillRecordId) throws SQLException {
        BigDecimal total = calculateTotalForUnitBill(unitBillRecordId);
        String query = "UPDATE unit_bill_record SET total_amount = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, unitBillRecordId);
            ps.executeUpdate();
        }
    }

    // ─── CALCULATION ────────────────────────────────────────────────────────────

    /**
     * Calculate the total for a flat's bill by summing BillLineItem amounts.
     * This is the DB-driven replacement for the old hardcoded BillingCalculationService.
     */
    public BigDecimal calculateTotalForUnitBill(int unitBillRecordId) throws SQLException {
        String query = "SELECT COALESCE(SUM(bli.amount), 0) AS total " +
                "FROM bill_line_item bli WHERE bli.unit_bill_record_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, unitBillRecordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get a summary of a flat's bill for a month:
     * total, fine, grand total, status, paid date, and line item breakdown.
     */
    public Map<String, Object> getBillSummaryForFlat(int flatId, String month, int year) throws SQLException {
        UnitBillRecord record = getUnitBillRecordByFlat(flatId, month, year);
        if (record == null) {
            return null;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("unit_bill_record_id", record.getId());
        summary.put("flat_id", record.getFlat_id());
        summary.put("status", record.getStatus());
        summary.put("total_amount", record.getTotal_amount());
        summary.put("fine_amount", record.getFine_amount() != null ? record.getFine_amount() : BigDecimal.ZERO);
        summary.put("paid_date", record.getPaid_date());
        summary.put("month", record.getMonth());
        summary.put("year", record.getYear());

        // Grand total = total + fine
        BigDecimal grandTotal = record.getTotal_amount();
        if (record.getFine_amount() != null) {
            grandTotal = grandTotal.add(record.getFine_amount());
        }
        summary.put("grand_total", grandTotal);

        // Line item breakdown
        List<Map<String, Object>> lineItems = getLineItemsForUnitBill(record.getId());
        summary.put("line_items", lineItems);

        return summary;
    }

    // ─── PRIVATE HELPERS ────────────────────────────────────────────────────────

    /**
     * Filter charge types by occupancy:
     *   OWNER  → charges with applicable_to = 'ALL' or 'OWNER'
     *   TENANT → charges with applicable_to = 'ALL' or 'TENANT'
     */
    private List<ChargeType> filterChargesByOccupancy(List<ChargeType> allCharges, String occupancyType) {
        List<ChargeType> filtered = new ArrayList<>();
        for (ChargeType ct : allCharges) {
            String applicableTo = ct.getApplicable_to() != null ? ct.getApplicable_to().toUpperCase() : "ALL";
            if ("ALL".equals(applicableTo) || applicableTo.equals(occupancyType)) {
                filtered.add(ct);
            }
        }
        return filtered;
    }

    private void deleteExistingBills(Connection conn, int societyId, String month, int year) throws SQLException {
        // Delete bill_line_item
        String deleteLineItems = "DELETE FROM bill_line_item WHERE unit_bill_record_id IN " +
                "(SELECT id FROM unit_bill_record WHERE bill_id IN " +
                "(SELECT id FROM bill WHERE sid = ? AND month = ? AND year = ?))";
        try (PreparedStatement ps = conn.prepareStatement(deleteLineItems)) {
            ps.setInt(1, societyId);
            ps.setString(2, month);
            ps.setInt(3, year);
            ps.executeUpdate();
        }

        // Delete unit_bill_record
        String deleteUnitBills = "DELETE FROM unit_bill_record WHERE bill_id IN " +
                "(SELECT id FROM bill WHERE sid = ? AND month = ? AND year = ?)";
        try (PreparedStatement ps = conn.prepareStatement(deleteUnitBills)) {
            ps.setInt(1, societyId);
            ps.setString(2, month);
            ps.setInt(3, year);
            ps.executeUpdate();
        }

        // Delete bill
        String deleteBill = "DELETE FROM bill WHERE sid = ? AND month = ? AND year = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteBill)) {
            ps.setInt(1, societyId);
            ps.setString(2, month);
            ps.setInt(3, year);
            ps.executeUpdate();
        }
    }

    private int createBill(Connection conn, int societyId, String month, int year, String dueDate) throws SQLException {
        String query = "INSERT INTO bill (sid, due_date, month, year) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, societyId);
            ps.setDate(2, Date.valueOf(dueDate));
            ps.setString(3, month);
            ps.setInt(4, year);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create Bill record");
    }

    private int createUnitBillRecord(Connection conn, int billId, int flatId,
                                      String status, BigDecimal totalAmount,
                                      String month, int year) throws SQLException {
        String query = "INSERT INTO unit_bill_record (bill_id, flat_id, status, total_amount, month, year) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, billId);
            ps.setInt(2, flatId);
            ps.setString(3, status);
            ps.setBigDecimal(4, totalAmount);
            ps.setString(5, month);
            ps.setInt(6, year);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create UnitBillRecord");
    }

    private void createBillLineItem(Connection conn, int unitBillRecordId,
                                     int chargeTypeHistoryId, BigDecimal amount) throws SQLException {
        String query = "INSERT INTO bill_line_item (unit_bill_record_id, charge_type_history_id, amount) " +
                "VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, unitBillRecordId);
            ps.setInt(2, chargeTypeHistoryId);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
        }
    }

    private UnitBillRecord mapUnitBillRecord(ResultSet rs) throws SQLException {
        UnitBillRecord record = new UnitBillRecord();
        record.setId(rs.getInt("id"));
        record.setBill_id(rs.getInt("bill_id"));
        record.setFlat_id(rs.getInt("flat_id"));
        record.setStatus(rs.getString("status"));
        record.setTotal_amount(rs.getBigDecimal("total_amount"));
        record.setFine_amount(rs.getBigDecimal("fine_amount"));
        java.sql.Date paidDate = rs.getDate("paid_date");
        record.setPaid_date(paidDate != null ? paidDate.toLocalDate() : null);
        record.setMonth(rs.getString("month"));
        record.setYear(rs.getInt("year"));
        return record;
    }

    public boolean isAdmin(int memId) {
        String sql = "SELECT isadmin FROM resident WHERE mem_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("isadmin");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
