package com.example.societyhub.service;

import com.example.societyhub.model.ChargeType;
import com.example.societyhub.model.ChargeTypeHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing master ChargeType records (CRUD)
 * and snapshotting them into ChargeTypeHistory at billing time.
 */
@Service
public class ChargeTypeService {

    private final DataSource dataSource;

    @Autowired
    public ChargeTypeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────────

    /**
     * Add a new master charge type for a society.
     * Returns the auto-generated id.
     */
    public int addChargeType(ChargeType chargeType) throws SQLException {
        String query = "INSERT INTO charge_type (society_id, name, default_amount, applicable_to, is_active) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);

            ps.setInt(1, chargeType.getSociety_id());
            ps.setString(2, chargeType.getName());
            ps.setBigDecimal(3, chargeType.getDefault_amount());
            ps.setString(4, chargeType.getApplicable_to() != null ? chargeType.getApplicable_to() : "ALL");
            ps.setBoolean(5, chargeType.getIs_active() != null ? chargeType.getIs_active() : true);
            ps.executeUpdate();

            int generatedId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

            conn.commit();
            return generatedId;
        }
    }

    /**
     * Get all active charge types for a society.
     */
    public List<ChargeType> getChargeTypesBySociety(int societyId) throws SQLException {
        List<ChargeType> chargeTypes = new ArrayList<>();
        String query = "SELECT id, society_id, name, default_amount, applicable_to, is_active " +
                "FROM charge_type WHERE society_id = ? AND is_active = true ORDER BY name";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    chargeTypes.add(mapChargeType(rs));
                }
            }
        }
        return chargeTypes;
    }

    /**
     * Get all charge types for a society (including inactive).
     */
    public List<ChargeType> getAllChargeTypesBySociety(int societyId) throws SQLException {
        List<ChargeType> chargeTypes = new ArrayList<>();
        String query = "SELECT id, society_id, name, default_amount, applicable_to, is_active " +
                "FROM charge_type WHERE society_id = ? ORDER BY name";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    chargeTypes.add(mapChargeType(rs));
                }
            }
        }
        return chargeTypes;
    }

    /**
     * Get a single charge type by id.
     */
    public ChargeType getChargeTypeById(int id) throws SQLException {
        String query = "SELECT id, society_id, name, default_amount, applicable_to, is_active " +
                "FROM charge_type WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapChargeType(rs);
                }
            }
        }
        return null;
    }

    /**
     * Update an existing charge type's name, amount, and applicability.
     */
    public void updateChargeType(ChargeType chargeType) throws SQLException {
        String query = "UPDATE charge_type SET name = ?, default_amount = ?, applicable_to = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setString(1, chargeType.getName());
            ps.setBigDecimal(2, chargeType.getDefault_amount());
            ps.setString(3, chargeType.getApplicable_to());
            ps.setInt(4, chargeType.getId());
            ps.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Soft-delete a charge type (set is_active = false).
     */
    public void deactivateChargeType(int id) throws SQLException {
        String query = "UPDATE charge_type SET is_active = false WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setInt(1, id);
            ps.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Re-activate a previously deactivated charge type.
     */
    public void activateChargeType(int id) throws SQLException {
        String query = "UPDATE charge_type SET is_active = true WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setInt(1, id);
            ps.executeUpdate();
            conn.commit();
        }
    }

    // ─── CHARGE TYPE FILTERING ──────────────────────────────────────────────────

    /**
     * Get charge types applicable to a specific occupancy type.
     * For OWNER-occupied flats: returns charges with applicable_to IN ('ALL', 'OWNER')
     * For TENANT-occupied flats: returns charges with applicable_to IN ('ALL', 'TENANT')
     */
    public List<ChargeType> getApplicableChargeTypes(int societyId, String occupancyType) throws SQLException {
        List<ChargeType> chargeTypes = new ArrayList<>();
        String query = "SELECT id, society_id, name, default_amount, applicable_to, is_active " +
                "FROM charge_type " +
                "WHERE society_id = ? AND is_active = true " +
                "AND (applicable_to = 'ALL' OR applicable_to = ?) " +
                "ORDER BY name";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societyId);
            ps.setString(2, occupancyType.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    chargeTypes.add(mapChargeType(rs));
                }
            }
        }
        return chargeTypes;
    }

    // ─── HISTORY SNAPSHOT ───────────────────────────────────────────────────────

    /**
     * Snapshot a ChargeType into ChargeTypeHistory for a specific billing month/year.
     * This "freezes" the name, amount, and applicability so future edits to the
     * master ChargeType don't affect past bills.
     * Returns the auto-generated history_id.
     */
    public int createChargeTypeHistorySnapshot(ChargeType chargeType, int societyId,
                                                String month, int year) throws SQLException {
        String query = "INSERT INTO charge_type_history " +
                "(charge_type_id, society_id, name_at_billing, amount_at_billing, applicable_to, month, year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);

            ps.setInt(1, chargeType.getId());
            ps.setInt(2, societyId);
            ps.setString(3, chargeType.getName());
            ps.setBigDecimal(4, chargeType.getDefault_amount());
            ps.setString(5, chargeType.getApplicable_to());
            ps.setString(6, month);
            ps.setInt(7, year);
            ps.executeUpdate();

            int generatedId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

            conn.commit();
            return generatedId;
        }
    }

    /**
     * Create history snapshot using a shared connection (for use within transactions).
     */
    public int createChargeTypeHistorySnapshot(Connection conn, ChargeType chargeType,
                                                int societyId, String month, int year) throws SQLException {
        String query = "INSERT INTO charge_type_history " +
                "(charge_type_id, society_id, name_at_billing, amount_at_billing, applicable_to, month, year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, chargeType.getId());
            ps.setInt(2, societyId);
            ps.setString(3, chargeType.getName());
            ps.setBigDecimal(4, chargeType.getDefault_amount());
            ps.setString(5, chargeType.getApplicable_to());
            ps.setString(6, month);
            ps.setInt(7, year);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to get generated history_id");
    }

    /**
     * Get all ChargeTypeHistory records for a given society + month + year.
     */
    public List<ChargeTypeHistory> getHistoryByMonth(int societyId, String month, int year) throws SQLException {
        List<ChargeTypeHistory> history = new ArrayList<>();
        String query = "SELECT history_id, charge_type_id, society_id, name_at_billing, " +
                "amount_at_billing, applicable_to, month, year " +
                "FROM charge_type_history WHERE society_id = ? AND month = ? AND year = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societyId);
            ps.setString(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapChargeTypeHistory(rs));
                }
            }
        }
        return history;
    }

    // ─── MAPPERS ────────────────────────────────────────────────────────────────

    private ChargeType mapChargeType(ResultSet rs) throws SQLException {
        ChargeType ct = new ChargeType();
        ct.setId(rs.getInt("id"));
        ct.setSociety_id(rs.getInt("society_id"));
        ct.setName(rs.getString("name"));
        ct.setDefault_amount(rs.getBigDecimal("default_amount"));
        ct.setApplicable_to(rs.getString("applicable_to"));
        ct.setIs_active(rs.getBoolean("is_active"));
        return ct;
    }

    private ChargeTypeHistory mapChargeTypeHistory(ResultSet rs) throws SQLException {
        ChargeTypeHistory h = new ChargeTypeHistory();
        h.setHistory_id(rs.getInt("history_id"));
        h.setCharge_type_id(rs.getInt("charge_type_id"));
        h.setSociety_id(rs.getInt("society_id"));
        h.setName_at_billing(rs.getString("name_at_billing"));
        h.setAmount_at_billing(rs.getBigDecimal("amount_at_billing"));
        h.setApplicable_to(rs.getString("applicable_to"));
        h.setMonth(rs.getString("month"));
        h.setYear(rs.getInt("year"));
        return h;
    }
}
