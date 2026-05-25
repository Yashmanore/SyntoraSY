package com.example.societyhub.service;

import com.example.societyhub.model.Flat;
import com.example.societyhub.model.FlatTenancy;
import com.example.societyhub.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FlatService {

    private final DataSource dataSource;

    @Autowired
    public FlatService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // FLAT CRUD

    /*
     * Get all flats for a given society.
     */
    public List<Flat> getFlatsBySociety(int societyId) throws SQLException {
        List<Flat> flats = new ArrayList<>();
        String query = "SELECT flat_id, flat_no, society_id, owner_mem_id, occupancy_type, mygate_no FROM flat WHERE society_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Flat flat = new Flat();
                    flat.setFlat_id(rs.getInt("flat_id"));
                    flat.setFlat_no(rs.getString("flat_no"));
                    flat.setSociety_id(rs.getInt("society_id"));
                    flat.setOwner_mem_id(rs.getString("owner_mem_id"));
                    flat.setOccupancy_type(rs.getString("occupancy_type"));
                    flat.setMygate_no(rs.getString("mygate_no"));
                    flats.add(flat);
                }
            }
        }
        return flats;
    }

    /*
     * Get a single flat by its ID.
     */
    public Flat getFlatById(int flatId) throws SQLException {
        String query = "SELECT flat_id, flat_no, society_id, owner_mem_id, occupancy_type, mygate_no FROM flat WHERE flat_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, flatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Flat flat = new Flat();
                    flat.setFlat_id(rs.getInt("flat_id"));
                    flat.setFlat_no(rs.getString("flat_no"));
                    flat.setSociety_id(rs.getInt("society_id"));
                    flat.setOwner_mem_id(rs.getString("owner_mem_id"));
                    flat.setOccupancy_type(rs.getString("occupancy_type"));
                    flat.setMygate_no(rs.getString("mygate_no"));
                    return flat;
                }
            }
        }
        return null;
    }

    /*
     * Get a flat by the owner's mygate_no (useful for resident dashboard).
     */
    public Flat getFlatByMygate(String mygateNo) throws SQLException {
        String query = "SELECT flat_id, flat_no, society_id, owner_mem_id, occupancy_type, mygate_no FROM flat WHERE mygate_no = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, mygateNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Flat flat = new Flat();
                    flat.setFlat_id(rs.getInt("flat_id"));
                    flat.setFlat_no(rs.getString("flat_no"));
                    flat.setSociety_id(rs.getInt("society_id"));
                    flat.setOwner_mem_id(rs.getString("owner_mem_id"));
                    flat.setOccupancy_type(rs.getString("occupancy_type"));
                    flat.setMygate_no(rs.getString("mygate_no"));
                    return flat;
                }
            }
        }
        return null;
    }

    /*
     * Add a new flat to a society.
     * Returns the auto-generated flat_id.
     */
    public int addFlat(Flat flat) throws SQLException {
        String query = "INSERT INTO flat (flat_no, society_id, owner_mem_id, occupancy_type, mygate_no) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);

            ps.setString(1, flat.getFlat_no());
            ps.setInt(2, flat.getSociety_id());
            ps.setString(3, flat.getOwner_mem_id());
            ps.setString(4, flat.getOccupancy_type() != null ? flat.getOccupancy_type() : "OWNER");
            ps.setString(5, flat.getMygate_no());
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

    // OCCUPANCY TYPE UPDATE (Owner ↔ Tenant toggle)

    /*
     * Update the occupancy_type of a flat.
     * Valid values: 'OWNER', 'TENANT'
     */
    public void updateOccupancyType(int flatId, String occupancyType) throws SQLException {
        String query = "UPDATE flat SET occupancy_type = ? WHERE flat_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setString(1, occupancyType);
            ps.setInt(2, flatId);
            ps.executeUpdate();
            conn.commit();
        }
    }

    // TENANT CRUD

    /*
     * Add a new tenant. Returns the auto-generated tenant_id.
     */
    public int addTenant(Tenant tenant) throws SQLException {
        String query = "INSERT INTO tenant (name, contact_no) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setString(1, tenant.getName());
            ps.setString(2, tenant.getContact_no());
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
     * Get a tenant by ID.
     */
    public Tenant getTenantById(int tenantId) throws SQLException {
        String query = "SELECT tenant_id, name, contact_no FROM tenant WHERE tenant_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Tenant tenant = new Tenant();
                    tenant.setTenant_id(rs.getInt("tenant_id"));
                    tenant.setName(rs.getString("name"));
                    tenant.setContact_no(rs.getString("contact_no"));
                    return tenant;
                }
            }
        }
        return null;
    }

    // FLAT TENANCY (linking Flat and Tenant)

    /*
     * Create a flat tenancy record linking a flat to a tenant.
     */
    public void addFlatTenancy(FlatTenancy tenancy) throws SQLException {
        String query = "INSERT INTO flat_tenancy (flat_id, tenant_id, lease_start_date, lease_end_date, security_deposit, rent_amount) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setInt(1, tenancy.getFlat_id());
            ps.setInt(2, tenancy.getTenant_id());
            ps.setDate(3, Date.valueOf(tenancy.getLease_start_date()));
            ps.setDate(4, tenancy.getLease_end_date() != null ? Date.valueOf(tenancy.getLease_end_date()) : null);
            ps.setBigDecimal(5, tenancy.getSecurity_deposit());
            ps.setBigDecimal(6, tenancy.getRent_amount());
            ps.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Get the current (active) tenancy for a flat.
     * "Active" = lease_end_date is NULL or in the future.
     */
    public FlatTenancy getActiveTenancy(int flatId) throws SQLException {
        String query = "SELECT ft.tenancy_id, ft.flat_id, ft.tenant_id, ft.lease_start_date, ft.lease_end_date, " +
                "ft.security_deposit, ft.rent_amount " +
                "FROM flat_tenancy ft " +
                "WHERE ft.flat_id = ? AND (ft.lease_end_date IS NULL OR ft.lease_end_date >= CURRENT_DATE) " +
                "ORDER BY ft.lease_start_date DESC LIMIT 1";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, flatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FlatTenancy tenancy = new FlatTenancy();
                    tenancy.setTenancy_id(rs.getInt("tenancy_id"));
                    tenancy.setFlat_id(rs.getInt("flat_id"));
                    tenancy.setTenant_id(rs.getInt("tenant_id"));
                    tenancy.setLease_start_date(rs.getDate("lease_start_date").toLocalDate());
                    Date endDate = rs.getDate("lease_end_date");
                    tenancy.setLease_end_date(endDate != null ? endDate.toLocalDate() : null);
                    tenancy.setSecurity_deposit(rs.getBigDecimal("security_deposit"));
                    tenancy.setRent_amount(rs.getBigDecimal("rent_amount"));
                    return tenancy;
                }
            }
        }
        return null;
    }

    /**
     * End (terminate) a tenancy by setting lease_end_date to today.
     */
    public void endTenancy(int tenancyId) throws SQLException {
        String query = "UPDATE flat_tenancy SET lease_end_date = CURRENT_DATE WHERE tenancy_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            ps.setInt(1, tenancyId);
            ps.executeUpdate();
            conn.commit();
        }
    }

    // COMPOSITE: Assign Tenant to Flat (transactional)
    // Creates Tenant + FlatTenancy + updates Flat occupancy
    // all in a single transaction.

    /**
     * Full workflow: mark a flat as TENANT-occupied.
     * 1. Insert a new Tenant record
     * 2. Insert a FlatTenancy record
     * 3. Update the Flat's occupancy_type to 'TENANT'
     *
     * All three happen in one transaction.
     */
    public void assignTenantToFlat(int flatId, Tenant tenant, FlatTenancy tenancy) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert Tenant
            String insertTenant = "INSERT INTO tenant (name, contact_no) VALUES (?, ?)";
            int tenantId;
            try (PreparedStatement ps = conn.prepareStatement(insertTenant, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, tenant.getName());
                ps.setString(2, tenant.getContact_no());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        tenantId = keys.getInt(1);
                    } else {
                        throw new SQLException("Failed to get generated tenant_id");
                    }
                }
            }

            // 2. Insert FlatTenancy
            String insertTenancy = "INSERT INTO flat_tenancy (flat_id, tenant_id, lease_start_date, lease_end_date, security_deposit, rent_amount) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertTenancy)) {
                ps.setInt(1, flatId);
                ps.setInt(2, tenantId);
                ps.setDate(3, Date.valueOf(tenancy.getLease_start_date()));
                ps.setDate(4, tenancy.getLease_end_date() != null ? Date.valueOf(tenancy.getLease_end_date()) : null);
                ps.setBigDecimal(5,
                        tenancy.getSecurity_deposit() != null ? tenancy.getSecurity_deposit() : BigDecimal.ZERO);
                ps.setBigDecimal(6, tenancy.getRent_amount() != null ? tenancy.getRent_amount() : BigDecimal.ZERO);
                ps.executeUpdate();
            }

            // 3. Update Flat occupancy_type
            String updateFlat = "UPDATE flat SET occupancy_type = 'TENANT' WHERE flat_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateFlat)) {
                ps.setInt(1, flatId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new SQLException("Failed to assign tenant to flat", e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Full workflow: revert a flat back to OWNER-occupied.
     * 1. End the active tenancy (set lease_end_date = today)
     * 2. Update the Flat's occupancy_type to 'OWNER'
     *
     * Both happen in one transaction.
     */
    public void revertToOwner(int flatId) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 1. End any active tenancy
            String endTenancy = "UPDATE flat_tenancy SET lease_end_date = CURRENT_DATE " +
                    "WHERE flat_id = ? AND (lease_end_date IS NULL OR lease_end_date >= CURRENT_DATE)";
            try (PreparedStatement ps = conn.prepareStatement(endTenancy)) {
                ps.setInt(1, flatId);
                ps.executeUpdate();
            }

            // 2. Update Flat occupancy_type
            String updateFlat = "UPDATE flat SET occupancy_type = 'OWNER' WHERE flat_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateFlat)) {
                ps.setInt(1, flatId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new SQLException("Failed to revert flat to owner", e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Get full tenant details for a flat (joins flat_tenancy + tenant).
     * Returns a Map with tenant info + tenancy info for easy consumption by
     * controllers.
     * Returns null if no active tenancy exists.
     */
    public Map<String, Object> getTenantDetails(int flatId) throws SQLException {
        String query = "SELECT t.tenant_id, t.name, t.contact_no, " +
                "ft.tenancy_id, ft.lease_start_date, ft.lease_end_date, ft.security_deposit, ft.rent_amount " +
                "FROM flat_tenancy ft " +
                "JOIN tenant t ON ft.tenant_id = t.tenant_id " +
                "WHERE ft.flat_id = ? AND (ft.lease_end_date IS NULL OR ft.lease_end_date >= CURRENT_DATE) " +
                "ORDER BY ft.lease_start_date DESC LIMIT 1";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, flatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("tenant_id", rs.getInt("tenant_id"));
                    details.put("tenant_name", rs.getString("name"));
                    details.put("tenant_contact", rs.getString("contact_no"));
                    details.put("tenancy_id", rs.getInt("tenancy_id"));
                    details.put("lease_start_date", rs.getDate("lease_start_date").toString());
                    Date endDate = rs.getDate("lease_end_date");
                    details.put("lease_end_date", endDate != null ? endDate.toString() : null);
                    BigDecimal deposit = rs.getBigDecimal("security_deposit");
                    details.put("security_deposit", deposit != null ? deposit.toString() : "0");
                    BigDecimal rent = rs.getBigDecimal("rent_amount");
                    details.put("rent_amount", rent != null ? rent.toString() : "0");
                    return details;
                }
            }
        }
        return null;
    }
}
