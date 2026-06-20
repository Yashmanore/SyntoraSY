import java.sql.*;

public class DBMigration {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/society_management";
        String user = "postgres";
        String password = "Yashm123@";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            // Check if tenant table exists
            ResultSet rs = stmt.executeQuery("SELECT to_regclass('public.tenant')");
            rs.next();
            String tenantTable = rs.getString(1);
            if (tenantTable == null) {
                System.out.println("tenant table does not exist. Creating it...");
                stmt.execute("CREATE TABLE tenant (tenant_id SERIAL PRIMARY KEY, name VARCHAR(255), contact_no VARCHAR(50), email VARCHAR(255), bill_type VARCHAR(20))");
                System.out.println("tenant table created.");
            } else {
                System.out.println("tenant table already exists.");
                // Add new columns if they don't exist yet
                try {
                    stmt.execute("ALTER TABLE tenant ADD COLUMN IF NOT EXISTS email VARCHAR(255)");
                    System.out.println("Added email column to tenant table.");
                } catch (SQLException e) {
                    System.out.println("email column might already exist: " + e.getMessage());
                }
                try {
                    stmt.execute("ALTER TABLE tenant ADD COLUMN IF NOT EXISTS bill_type VARCHAR(20)");
                    System.out.println("Added bill_type column to tenant table.");
                } catch (SQLException e) {
                    System.out.println("bill_type column might already exist: " + e.getMessage());
                }
            }

            // Alter resident table
            try {
                stmt.execute("ALTER TABLE resident ADD COLUMN is_tenant BOOLEAN DEFAULT FALSE, ADD COLUMN tenant_id INTEGER REFERENCES tenant(tenant_id)");
                System.out.println("Altered resident table successfully.");
            } catch (SQLException e) {
                System.out.println("Columns might already exist: " + e.getMessage());
            }

            // Create charge_type table
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS charge_type (id SERIAL PRIMARY KEY, society_id INTEGER, name VARCHAR(255), default_amount DECIMAL(10,2), applicable_to VARCHAR(50), is_active BOOLEAN DEFAULT TRUE)");
                System.out.println("charge_type table ready.");
            } catch (SQLException e) {
                System.out.println("charge_type table check error: " + e.getMessage());
            }

            // Create charge_type_history table
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS charge_type_history (history_id SERIAL PRIMARY KEY, charge_type_id INTEGER, society_id INTEGER, name_at_billing VARCHAR(255), amount_at_billing DECIMAL(10,2), applicable_to VARCHAR(50), month VARCHAR(20), year INTEGER)");
                System.out.println("charge_type_history table ready.");
            } catch (SQLException e) {
                System.out.println("charge_type_history table check error: " + e.getMessage());
            }

            // Create bill table
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS bill (id SERIAL PRIMARY KEY, sid INTEGER, due_date DATE, month VARCHAR(20), year INTEGER)");
                System.out.println("bill table ready.");
            } catch (SQLException e) {
                System.out.println("bill table check error: " + e.getMessage());
            }

            // Create unit_bill_record table
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS unit_bill_record (id SERIAL PRIMARY KEY, bill_id INTEGER REFERENCES bill(id), flat_id INTEGER, status VARCHAR(50), total_amount DECIMAL(10,2), fine_amount DECIMAL(10,2), paid_date DATE, month VARCHAR(20), year INTEGER)");
                System.out.println("unit_bill_record table ready.");
            } catch (SQLException e) {
                System.out.println("unit_bill_record table check error: " + e.getMessage());
            }

            // Create bill_line_item table
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS bill_line_item (id SERIAL PRIMARY KEY, unit_bill_record_id INTEGER REFERENCES unit_bill_record(id), charge_type_history_id INTEGER REFERENCES charge_type_history(history_id), amount DECIMAL(10,2))");
                System.out.println("bill_line_item table ready.");
            } catch (SQLException e) {
                System.out.println("bill_line_item table check error: " + e.getMessage());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
