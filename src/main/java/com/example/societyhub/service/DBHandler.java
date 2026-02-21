package com.example.societyhub.service;

import com.example.societyhub.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

@Service
public class DBHandler {


    @Autowired
    private MyGateService myGateService;

    private final DataSource dataSource;

    @Autowired
    public DBHandler(DataSource dataSource, MyGateService myGateService) {
        this.dataSource = dataSource;
        this.myGateService = myGateService;
    }

    public List<Map<String, String>> queryResident(int sid) throws SQLException {
        List<Map<String, String>> residents = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String query = "select mem_id, name, room_no, mr_ms, gender, age, contact_no, mygate_no, email, bhk from resident where sid = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                // Set the value for the 'sid' parameter
                preparedStatement.setInt(1, sid); // Assuming 'sid' is of type int, adjust if it's another type

                // Now execute the query
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Map<String, String> resident = new HashMap<>();
                        resident.put("mem_id", resultSet.getString("mem_id"));
                        resident.put("name", resultSet.getString("name"));
                        resident.put("room_no", resultSet.getString("room_no"));
                        resident.put("mr_ms", resultSet.getString("mr_ms"));
                        resident.put("gender", resultSet.getString("gender"));
                        resident.put("age", resultSet.getString("age"));
                        resident.put("contact_no", resultSet.getString("contact_no"));
                        resident.put("mygate_no", resultSet.getString("mygate_no"));
                        resident.put("email", resultSet.getString("email"));
                        resident.put("bhk", resultSet.getString("bhk"));
                        residents.add(resident);
                    }
                    connection.commit();
                } catch (Exception e) {
                    // Rollback the transaction in case of an error
                    connection.rollback();
                    e.printStackTrace();
                    throw new SQLException("Error ", e);
                }
            }
        }
        return residents;
    }

    public List<Resident> getResident(int sid) throws SQLException {
        List<Resident> residents = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String query = "select mem_id, name, room_no, mr_ms, gender, age, contact_no, mygate_no, email, bhk from resident where sid = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                // Set the value for the 'sid' parameter
                preparedStatement.setInt(1, sid); // Assuming 'sid' is of type int, adjust if it's another type

                // Now execute the query
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Resident resident = new Resident();
                        resident.setMem_id(resultSet.getString("mem_id"));
                        resident.setName(resultSet.getString("name"));
//                        resident.setRoom_no(Integer.parseInt(resultSet.getString("room_no")));
                        int roomNo = resultSet.getInt("room_no");
                        resident.setRoom_no(resultSet.wasNull() ? null : roomNo);
                        resident.setMr_ms(resultSet.getString("mr_ms"));
                        resident.setGender(resultSet.getString("gender"));
//                        resident.setAge(Integer.parseInt(resultSet.getString("age")));
                        int age = resultSet.getInt("age");
                        resident.setAge(resultSet.wasNull() ? null : age);
                        resident.setContactNo(resultSet.getString("contact_no"));
                        resident.setMygate_no(resultSet.getString("mygate_no"));
                        resident.setEmail(resultSet.getString("email"));
                        resident.setBhk(resultSet.getString("bhk"));
//                        resident.setStatus(resultSet.getString("status"));
                        residents.add(resident);

                        connection.commit();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new SQLException("Error ", e);
                }
            }
        }
        return residents;
    }

    public List<Resident> getResidentBillDetails(String month, int sid) throws SQLException {
        List<Resident> residents = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            // Query to fetch resident details along with the boolean status for the given month
            String query = "SELECT r.mem_id, r.name, r.room_no, r.mr_ms, r.gender, r.age, r.contact_no, " +
                    "r.mygate_no, r.email, r.bhk, rb." + month + " AS status " +
                    "FROM resident r " +
                    "JOIN resident_bill rb ON r.mygate_no = rb.mygate_no " +
                    "WHERE r.sid = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                // Set the value for the 'sid' parameter
                preparedStatement.setInt(1, sid);

                // Execute the query
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Resident resident = new Resident();
                        resident.setMem_id(resultSet.getString("mem_id"));
                        resident.setName(resultSet.getString("name"));
                        resident.setRoom_no(resultSet.getInt("room_no"));
                        resident.setMr_ms(resultSet.getString("mr_ms"));
                        resident.setGender(resultSet.getString("gender"));
                        resident.setAge(resultSet.getInt("age"));
                        resident.setContactNo(resultSet.getString("contact_no"));
                        resident.setMygate_no(resultSet.getString("mygate_no"));
                        resident.setEmail(resultSet.getString("email"));
                        resident.setBhk(resultSet.getString("bhk"));
                        resident.setMonth(month);

                        // Retrieve and set the boolean value for the given month
                        resident.setStatus(String.valueOf(resultSet.getInt("status")));

                        residents.add(resident);
                    }
                    connection.commit();
                }
            } catch (Exception e) {
                // Rollback the transaction in case of an error
                connection.rollback();
                e.printStackTrace();
                throw new SQLException("Error ", e);
            }
        }
        return residents;
    }

    public Resident getResident(String mygate_no) throws SQLException {
        Resident resident = null; // Initialize as null

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String query = "select sid, name, room_no, mr_ms, gender, age, contact_no, email, bhk from resident where mygate_no = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, mygate_no); // Set the 'mygate_no' parameter

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) { // Check if data exists
                        resident = new Resident();
                        resident.setSid(resultSet.getInt("sid"));
                        resident.setName(resultSet.getString("name"));
                        resident.setRoom_no(resultSet.getInt("room_no"));
                        resident.setMr_ms(resultSet.getString("mr_ms"));
                        resident.setGender(resultSet.getString("gender"));
                        resident.setAge(resultSet.getInt("age"));
                        resident.setContactNo(resultSet.getString("contact_no"));
                        resident.setEmail(resultSet.getString("email"));
                        resident.setBhk(resultSet.getString("bhk"));
                    }
                    connection.commit();
                } catch (Exception e) {
                    // Rollback the transaction in case of an error
                    connection.rollback();
                    e.printStackTrace();
                    throw new SQLException("Error ", e);
                }
            }
        }
        return resident; // Return null if no data found
    }



    public int registerSociety(String name, String street, String landmark, String locality, String city, String state, String pincode, String country) throws SQLException {
        String insertQuery = "insert into society (name, street, landmark, locality, city, state, pincode, country) values (?, ?, ?, ?, ?, ?, ?, ?)";
        String selectQuery = "select sid from society where name = ? and street = ? and landmark = ? and locality = ? and pincode = ?";

        int societyId = -1;  // Default value to indicate if no ID is found

        try (Connection connection = dataSource.getConnection();

             PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
            connection.setAutoCommit(false);
            // Set parameters for the INSERT query
            insertStatement.setString(1, name);
            insertStatement.setString(2, street);
            insertStatement.setString(3, landmark);
            insertStatement.setString(4, locality);
            insertStatement.setString(5, city);
            insertStatement.setString(6, state);
            insertStatement.setString(7, pincode);
            insertStatement.setString(8, country);

            // Execute the INSERT query
            insertStatement.executeUpdate();

            connection.commit();

            // Set parameters for the SELECT query
            selectStatement.setString(1, name);
            selectStatement.setString(2, street);
            selectStatement.setString(3, landmark);
            selectStatement.setString(4, locality);
            selectStatement.setString(5, pincode);


            // Execute the SELECT query to retrieve the sid
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    societyId = resultSet.getInt("sid");
                }
            }
        } catch (SQLException e) {

            e.printStackTrace();
            throw e;  // Re-throw exception for further handling
        }

        return societyId;  // Return the retrieved sid, or -1 if not found
    }

    public boolean societyExists(String name) throws SQLException {
        String query = "select count(*) from society where name = ? ";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }
        return false;
    }

    // Method for retrieving societies
    public List<Society> getAllSocieties() throws SQLException {
        List<Society> societies = new ArrayList<>();
        String query = "select sid, name, street, landmark, pincode, city, state, country from society";
        System.out.println("Hello 1");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            connection.setAutoCommit(false);
            System.out.println("Hello 2");
            while (resultSet.next()) {
                Society society = new Society();
                society.setSid(resultSet.getInt("sid"));
                society.setName(resultSet.getString("name"));
                society.setStreet(resultSet.getString("street"));
                society.setLandmark(resultSet.getString("landmark"));
                society.setPincode(resultSet.getString("pincode"));
                society.setCity(resultSet.getString("city"));
                society.setState(resultSet.getString("state"));
                society.setCountry(resultSet.getString("country"));
                societies.add(society);
                System.out.println("Society.getId: " + society.getSid());
            }
        }
        return societies;
    }

    // Method to check if the user already exists
    public boolean adminExists(String email) throws Exception {
        System.out.println("Welcome 1");
        String query = "select count(*) from login where email_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            System.out.println("Welcome 2");
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0; // If count > 0, user exists
            }
        }
        return false;
    }

    // Method to register a new user
    public void registerAdmin(String email, String hashedPassword) throws Exception {
        System.out.println("Welcome 3");
        String query = "insert into login (email_id, password) values (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            System.out.println("Welcome 4");
            statement.setString(1, email);
            statement.setString(2, hashedPassword);
            statement.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            // Rollback the transaction in case of an error
//            connection.rollback();
            e.printStackTrace();
            throw new SQLException("Error ", e);
        }
    }

    // Method to add a new resident
    public void addResident(Resident resident) throws Exception {
        System.out.println("Welcome 3");

        // SQL query to insert a new resident
        String query = "insert into resident (mem_id, sid, name, room_no, mr_ms, gender, age, contact_no, isadmin, mygate_no, bhk, email) values (?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?, ?)";

        // Connect to the database
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            // Get the current highest mem_id for the society
            String getMaxMemIdQuery = "select coalesce(max(mem_id), 0) from resident where sid = ?";
            try (PreparedStatement getMaxMemIdStatement = connection.prepareStatement(getMaxMemIdQuery)) {
                getMaxMemIdStatement.setInt(1, (resident.getSid()));
                ResultSet resultSet = getMaxMemIdStatement.executeQuery();

                int currentMaxMemId = 0;
                if (resultSet.next()) {
                    currentMaxMemId = resultSet.getInt(1);
                }

                // Prepare for the next mem_id (e.g., sid001, sid002, etc.)
                int mem_id = currentMaxMemId == 0 ? Integer.parseInt(resident.getSid() + "001") : currentMaxMemId + 1;

                // Generate a unique MyGate number
                String myGateNo = myGateService.generateUniqueMyGateNumber(new HashSet<>());

                // Prepare statement for resident insertion
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    System.out.println("Welcome 4");

                    // Set parameters for the SQL insert query
                    statement.setInt(1, mem_id);
                    statement.setInt(2, (resident.getSid()));
                    statement.setString(3, resident.getName());
                    statement.setInt(4, resident.getRoom_no());
                    statement.setString(5, resident.getMr_ms());
                    statement.setString(6, resident.getGender());
                    statement.setInt(7, resident.getAge());
                    statement.setString(8, resident.getContactNo());
                    statement.setString(9, myGateNo);
                    statement.setString(10, resident.getBhk());
                    System.out.println("BHK: " + resident.getBhk());
                    statement.setString(11, resident.getEmail());
                    System.out.println("Email: " + resident.getEmail());
//                    statement.setString(12, resident.getStatus());
//                    System.out.println("Status: " + resident.getStatus());

                    // Execute the insert query
                    statement.executeUpdate();

                    connection.commit();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Error adding resident: " + e.getMessage());
        }
    }

    public void deleteResident(String mygate_no) throws Exception {
        System.out.println("Welcome 3");
        String query = "delete from resident where mygate_no = ? ";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            System.out.println("On the way to delete");
            statement.setString(1, mygate_no);
            statement.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            // Rollback the transaction in case of an error
//            connection.rollback();
            e.printStackTrace();
            throw new SQLException("Error ", e);
        }
    }

    public void updateResident(Resident resident) throws SQLException {
        // Updated SQL query to reference the correct column name
        String sql = "update resident set room_no = ?, mr_ms = ?, gender = ?, age = ?, contact_no = ?, bhk = ?, email = ? where mygate_no = ?";

        try (Connection connection = dataSource.getConnection(); // Implement your database connection method
             PreparedStatement statement = connection.prepareStatement(sql)){
            connection.setAutoCommit(false);

            statement.setInt(1, resident.getRoom_no());
            statement.setString(2, resident.getMr_ms());
            statement.setString(3, resident.getGender());
            statement.setInt(4, resident.getAge());
            statement.setString(5, resident.getContactNo());
            statement.setString(6, resident.getBhk());
            statement.setString(7, resident.getEmail());
//            statement.setString(8, resident.getStatus());
            statement.setString(8, resident.getMygate_no());

            // Execute the insert query
            statement.executeUpdate();

            connection.commit();

        } catch (Exception e) {
            // Rollback the transaction in case of an error
//            connection.rollback();
            e.printStackTrace();
            throw new SQLException("Error ", e);
        }
    }

    public void updateResidentBill(String mygateNo, int year, String month, int statusValue) throws SQLException {
        // Map the month to the corresponding column name
        String columnName = month.toLowerCase(); // Ensure the month matches the column naming convention
        System.out.println("Status value: " + statusValue);
        System.out.println("Mygate value: " + mygateNo);
        System.out.println("Month value: " + columnName);
        System.out.println("Year: " + year);

        String sql = "update resident_bill set " + columnName + " = ? where mygate_no = ? and year = ?";

        try (Connection connection = dataSource.getConnection(); // Implement your database connection method
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);

            statement.setInt(1, statusValue);
            statement.setString(2, mygateNo);
            statement.setInt(3, year);

            // Execute the update query
            int rows = statement.executeUpdate();
            System.out.println("Rows updated: " + rows);

            connection.commit();
        } catch (Exception e) {
            // Rollback the transaction in case of an error
//            connection.rollback();
            e.printStackTrace();
            throw new SQLException("Error ", e);
        }
    }


    public void update(int sid, String name, String contact_no, String email_id) throws Exception {
        String memIdQuery = "select coalesce(max(mem_id), 0) from resident where sid = ?";
        String insertResidentQuery = "insert into resident (mem_id, sid, name, contact_no, isadmin) values (?, ?, ?, ?, true)";
        String checkLoginQuery = "select id, mem_id from login where email_id = ?";
        String updateLoginQuery = "update login set mem_id = ? where email_id = ?";
        String validateSocietyQuery = "select count(*) from society where sid = ?";
        String updateSocietyQuery = "update society set admin_id = ? where sid = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement validateSocietyStatement = connection.prepareStatement(validateSocietyQuery);
             PreparedStatement getMaxMemIdStatement = connection.prepareStatement(memIdQuery);
             PreparedStatement insertResidentStatement = connection.prepareStatement(insertResidentQuery);
             PreparedStatement checkLoginStatement = connection.prepareStatement(checkLoginQuery);
             PreparedStatement updateLoginStatement = connection.prepareStatement(updateLoginQuery);
             PreparedStatement updateSocietyStatement = connection.prepareStatement(updateSocietyQuery)) {
            connection.setAutoCommit(false);

            // Validate if sid exists in society
            validateSocietyStatement.setInt(1, sid);
            ResultSet societyResultSet = validateSocietyStatement.executeQuery();
            if (societyResultSet.next() && societyResultSet.getInt(1) == 0) {
                throw new Exception("The provided sid does not exist in the society table.");
            }

            // Get the current highest mem_id for this society
            getMaxMemIdStatement.setInt(1, sid);
            ResultSet resultSet = getMaxMemIdStatement.executeQuery();
            int currentMaxMemId = 0;
            if (resultSet.next()) {
                currentMaxMemId = resultSet.getInt(1);
            }

            // Calculate the next mem_id
            int nextMemId = currentMaxMemId == 0 ? Integer.parseInt(sid + "001") : currentMaxMemId + 1;

            // Insert into resident table
            insertResidentStatement.setInt(1, nextMemId);
            insertResidentStatement.setInt(2, sid);
            insertResidentStatement.setString(3, name);
            insertResidentStatement.setString(4, contact_no);
            insertResidentStatement.executeUpdate();

            // Check if the email_id already exists in login table
            checkLoginStatement.setString(1, email_id);
            ResultSet loginResultSet = checkLoginStatement.executeQuery();
            int adminId = -1;

            if (loginResultSet.next()) {
                // Email exists, get the existing id and mem_id
                adminId = loginResultSet.getInt("id");
                int existingMemId = loginResultSet.getInt("mem_id");

                // Update the existing login entry with new mem_id if necessary
                if (existingMemId != nextMemId) {
                    updateLoginStatement.setInt(1, nextMemId);
                    updateLoginStatement.setString(2, email_id);
                    updateLoginStatement.executeUpdate();
                }
            } else {
                // Email does not exist, handle this case if needed
                throw new Exception("The provided email_id does not exist in the login table.");
            }

            // Update society table with the new admin_id
            updateSocietyStatement.setInt(1, adminId);
            updateSocietyStatement.setInt(2, sid);
            updateSocietyStatement.executeUpdate();

            connection.commit();

            System.out.println("Admin registered successfully with mem_id: " + nextMemId + " and admin_id: " + adminId);

        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Handle exception properly
        }
    }

    public Admin getAdminDetails(String email) throws SQLException {
        String queryMemId = "select mem_id from login where email_id = ?";
        String queryResidentDetails = "select name, contact_no, sid from resident where mem_id = ?";
        Admin admin = new Admin(); // Assuming Admin class has a no-argument constructor

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmtMemId = conn.prepareStatement(queryMemId);
             PreparedStatement pstmtResidentDetails = conn.prepareStatement(queryResidentDetails)) {

            // Step 1: Get mem_id from the login table
            pstmtMemId.setString(1, email);
            ResultSet rsMemId = pstmtMemId.executeQuery();

            if (rsMemId.next()) {
                int mem_id = rsMemId.getInt("mem_id");
                admin.setEmail_id(email);  // Set the email in Admin
                admin.setMem_id(mem_id);   // Set the mem_id in Admin

                // Step 2: Use mem_id to get the name, contact_no, and sid from the resident table
                pstmtResidentDetails.setInt(1, mem_id);
                ResultSet rsResidentDetails = pstmtResidentDetails.executeQuery();

                if (rsResidentDetails.next()) {
                    admin.setName(rsResidentDetails.getString("name"));             // Set name in Admin
                    admin.setContact_no(rsResidentDetails.getString("contact_no")); // Set contact_no in Admin
                    admin.setSocietyId(rsResidentDetails.getInt("sid"));            // Set society_id (sid) in Admin
                } else {
                    System.err.println("No resident details found for mem_id: " + mem_id);
                    return null; // If no resident details are found
                }
            } else {
                System.err.println("No mem_id found for email: " + email);
                return null; // If no mem_id is found for the email
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException("Unexpected error", e);
        }
        return admin; // Return the populated Admin object
    }

    public List<Admin> getAdmin(int sid) throws SQLException {
        List<Admin> admins = new ArrayList<>();
        String queryAdmin = "select name, contact_no, mygate_no, email from resident where sid = ? and isadmin = true";
        Admin admin = new Admin();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmtAdmin= conn.prepareStatement(queryAdmin)) {

            // Step 1: Get mem_id from the login table
            pstmtAdmin.setInt(1, sid);
            ResultSet rsAdmin = pstmtAdmin.executeQuery();

            if (rsAdmin.next()) {
                // Fetch and set other fields
                admin.setName(rsAdmin.getString("name")); // Set name in Admin
                admin.setContact_no(rsAdmin.getString("contact_no")); // Set contact_no in Admin
                admin.setMygate_no(rsAdmin.getString("mygate_no")); // Set mygate_no in Admin
                admin.setEmail_id(rsAdmin.getString("email")); // Set email in Admin
                admins.add(admin);
            } else {
                System.err.println("No admin found for sid: " + sid);
                return null; // If no mem_id is found for the email
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException("Unexpected error", e);
        }
        return admins; // Return the populated Admin object
    }


    public Boolean isDataUploaded(int sid) throws Exception {
        String query = "select data_uploaded from society where sid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            System.out.println("Query: " + query); // Debugging
            System.out.println("SID: " + sid);     // Debugging
            preparedStatement.setInt(1, sid);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                System.out.println("Data uploaded: " + resultSet.getBoolean("data_uploaded"));
                return resultSet.getBoolean("data_uploaded");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Debugging
            throw new Exception("Error checking if data is uploaded", e);
        }
        return null;
    }

    public String getPasswordByEmail(String email) throws Exception {
        String query = "select password from login where email_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("password"); // Return hashed password
            }
        }
        return null;
    }

    public String getPasswordByMyGateNo(String mygate_no) throws Exception {
        String query = "select password from resident where mygate_no = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, mygate_no);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("password"); // Return hashed password
            }
        }
        return null;
    }

    public String getAdminPassword(String username) throws Exception {
        String query = "select password from admin where username = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            System.out.println("Username: " + username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("password"); // Return hashed password
            }
        }
        return null;
    }

    public Society getSocietyBySid(int sid) throws Exception {
        String query = "select name, street, landmark, locality, pincode, city, state from society where sid = ?";
        Society society = new Society();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, sid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                society.setName(resultSet.getString("name"));
                society.setStreet(resultSet.getString("street"));
                society.setLandmark(resultSet.getString("landmark"));
                society.setLocality(resultSet.getString("locality"));
                society.setPincode(resultSet.getString("pincode"));
                society.setCity(resultSet.getString("city"));
            }
        }
        return society;
    }

    public void insertOrUpdateBill(Bill bill) throws SQLException {
        // Check if a bill already exists for the given sid
        String selectQuery = "SELECT COUNT(*) FROM bill WHERE sid = ?";
        String insertQuery = "INSERT INTO bill (sid, maintenance_contribution, housing_board_contribution, property_tax_contribution, sinking_fund, reserve_mhada_service_charge, sub_charge, fine, building_dev_fund, other, due_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateQuery = "UPDATE bill SET maintenance_contribution = ?, housing_board_contribution = ?, property_tax_contribution = ?, sinking_fund = ?, reserve_mhada_service_charge = ?, sub_charge = ?, fine = ?, building_dev_fund = ?, other = ?, due_date = ?  WHERE sid = ?";

        try (Connection connection = dataSource.getConnection()) {
            // Check if a bill already exists for the given sid
            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                connection.setAutoCommit(false);
                selectStatement.setInt(1, bill.getSid());
                ResultSet resultSet = selectStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);

                if (count > 0) {
                    // Bill already exists for this sid, update it
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setInt(1, bill.getMaintenance_contribution());
                        updateStatement.setInt(2, bill.getHousing_board_contribution());
                        updateStatement.setInt(3, bill.getProperty_tax_contribution());
                        updateStatement.setInt(4, bill.getSinking_fund());
                        updateStatement.setInt(5, bill.getReserve_mhada_service_charge());
                        updateStatement.setInt(6, bill.getSub_charge());
                        updateStatement.setInt(7, bill.getFine());
                        updateStatement.setInt(8, bill.getBuilding_dev_fund());
                        updateStatement.setInt(9, bill.getOther());
                        updateStatement.setDate(10, Date.valueOf(bill.getDue_date()));
                        updateStatement.setInt(11, bill.getSid());
                        updateStatement.executeUpdate();
                        connection.commit();
                    }
                } else {
                    // No bill exists, insert a new one
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                        insertStatement.setInt(1, bill.getSid());
                        insertStatement.setInt(2, bill.getMaintenance_contribution());
                        insertStatement.setInt(3, bill.getHousing_board_contribution());
                        insertStatement.setInt(4, bill.getProperty_tax_contribution());
                        insertStatement.setInt(5, bill.getSinking_fund());
                        insertStatement.setInt(6, bill.getReserve_mhada_service_charge());
                        insertStatement.setInt(7, bill.getSub_charge());
                        insertStatement.setInt(8, bill.getFine());
                        insertStatement.setInt(9, bill.getBuilding_dev_fund());
                        insertStatement.setInt(10, bill.getOther());
                        insertStatement.setDate(11, Date.valueOf(bill.getDue_date()));
                        insertStatement.executeUpdate();

                        connection.commit();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Failed to insert or update bill details", e);
        }
    }


    public Bill fetchBillDetails(int sid) throws SQLException {
        Bill bill = null;
        try (Connection connection = dataSource.getConnection()) {
            String query = "select * from bill where sid = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, sid);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        bill = new Bill();
                        bill.setSid(resultSet.getInt("id"));
                        bill.setSid(resultSet.getInt("sid"));
                        bill.setMaintenance_contribution(resultSet.getInt("maintenance_contribution"));
                        bill.setHousing_board_contribution(resultSet.getInt("housing_board_contribution"));
                        bill.setProperty_tax_contribution(resultSet.getInt("property_tax_contribution"));
                        bill.setSinking_fund(resultSet.getInt("sinking_fund"));
                        bill.setReserve_mhada_service_charge(resultSet.getInt("reserve_mhada_service_charge"));
                        bill.setSub_charge(resultSet.getInt("sub_charge"));
                        bill.setFine(resultSet.getInt("fine"));
                        bill.setBuilding_dev_fund(resultSet.getInt("building_dev_fund"));
                        bill.setOther(resultSet.getInt("other"));
                        bill.setDue_date(String.valueOf(resultSet.getDate("due_date")));
                    }
                }
            }
        }
        return bill;
    }

    public Bill fetchBill(String mygate_no, String month, int sid) throws SQLException {
        Bill bill = null;
        try (Connection connection = dataSource.getConnection()) {
            String query = "select * from bill where sid = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, sid);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        bill = new Bill();
                        bill.setSid(resultSet.getInt("id"));
                        bill.setSid(resultSet.getInt("sid"));
                        bill.setMaintenance_contribution(resultSet.getInt("maintenance_contribution"));
                        bill.setHousing_board_contribution(resultSet.getInt("housing_board_contribution"));
                        bill.setProperty_tax_contribution(resultSet.getInt("property_tax_contribution"));
                        bill.setSinking_fund(resultSet.getInt("sinking_fund"));
                        bill.setReserve_mhada_service_charge(resultSet.getInt("reserve_mhada_service_charge"));
                        bill.setSub_charge(resultSet.getInt("sub_charge"));
                        bill.setFine(resultSet.getInt("fine"));
                        bill.setBuilding_dev_fund(resultSet.getInt("building_dev_fund"));
                        bill.setOther(resultSet.getInt("other"));
                        bill.setDue_date(String.valueOf(resultSet.getDate("due_date")));
                    }
                }
            }
        }
        return bill;
    }


    // Method to get the next bill number from the sequence
    public Integer getNextBillNumber() {
        Integer billNo = null;
        String query = "select nextval('seq_bill_no')"; // Use your sequence name here
        try (Connection connection = dataSource.getConnection(); // Implement your database connection method
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                billNo = resultSet.getInt(1); // Get the next value
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return billNo;
    }

    public boolean updateResidentPassword(String mygate_no, String hashedPassword) throws SQLException {
        // Updated SQL query to reference the correct column name
        String sql = "UPDATE resident SET password = ? WHERE mygate_no = ?";

        try (Connection connection = dataSource.getConnection(); // Implement your database connection method
             PreparedStatement statement = connection.prepareStatement(sql)){
            connection.setAutoCommit(false);

            statement.setString(1, hashedPassword);
            statement.setString(2, mygate_no); // Correctly bind the mygate_no parameter
            int rowsUpdated = statement.executeUpdate();

            connection.commit();
            return rowsUpdated > 0; // Return true if at least one row was updated

        }
    }

    public List<Note> getNotes(int sid) throws SQLException {
        List<Note> noteList = new ArrayList<>(); // Initialize the list to store To-Do items

        try (Connection connection = dataSource.getConnection()) {
            // Updated query to select all To-Do items for the given societyId (sid)
            String query = "SELECT title, message FROM note WHERE sid = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, sid); // Set the 'sid' parameter

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) { // Loop through all results
                        Note note = new Note();
                        note.setTitle(resultSet.getString("title"));
                        note.setMessage(resultSet.getString("message"));
                        noteList.add(note); // Add each ToDo to the list
                    }
                }
            }
        }
        return noteList; // Return the list of ToDo items (could be empty if no data found)
    }

    public List<Announcement> getAnnouncement(int sid) throws SQLException {
        List<Announcement> announcementList = new ArrayList<>(); // Initialize the list to store To-Do items

        try (Connection connection = dataSource.getConnection()) {
            // Updated query to select all To-Do items for the given societyId (sid)
            String query = "SELECT title, message, category, created_at FROM announcements WHERE sid = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, sid); // Set the 'sid' parameter

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) { // Loop through all results
                        Announcement announcement = new Announcement();
                        announcement.setTitle(resultSet.getString("title"));
                        announcement.setCategory(resultSet.getString("category"));
                        announcement.setMessage(resultSet.getString("message"));
//                        announcement.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                        Timestamp ts = resultSet.getTimestamp("created_at");
                        if (ts != null) {
                            announcement.setCreatedAt(ts.toLocalDateTime());
                        }
                        announcementList.add(announcement); // Add each ToDo to the list
                    }
                }
            }
        }
        return announcementList; // Return the list of ToDo items (could be empty if no data found)
    }


    public void addNote(String title, String message, int sid) throws Exception {
        String query = "INSERT INTO note (sid, title, message) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                connection.setAutoCommit(false);
                statement.setInt(1, sid);
                statement.setString(2, title);
                statement.setString(3, message);
                statement.executeUpdate();

                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Error adding note: " + e.getMessage());
        }
    }

    public void addAnnouncement(String title, String message, String category, int sid) throws Exception {
        String query = "INSERT INTO announcements (sid, title, message, category, is_active) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                connection.setAutoCommit(false);
                statement.setInt(1, sid);
                statement.setString(2, title);
                statement.setString(3, message);
                statement.setString(4, category);
                statement.setBoolean(5, true);
                statement.executeUpdate();

                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Error adding announcement: " + e.getMessage());
        }
    }


    public void deleteNote(int sid, String title) throws Exception {
        System.out.println("Welcome 3");
        String query = "delete from note where sid = ? and title = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            System.out.println("On the way to delete");
            statement.setInt(1, sid);
            statement.setString(2, title);
            statement.executeUpdate();

            int rowsAffected = statement.executeUpdate();

            connection.commit();
            System.out.println("Rows affected: " + rowsAffected);
        }
    }

    public void deleteAnnouncement(int sid, String title) throws Exception {
        System.out.println("Welcome 3");
        String query = "delete from announcements where sid = ? and title = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            System.out.println("On the way to delete");
            statement.setInt(1, sid);
            statement.setString(2, title);
            statement.executeUpdate();

            int rowsAffected = statement.executeUpdate();

            connection.commit();
            System.out.println("Rows affected: " + rowsAffected);
        }
    }

    public List<Complaint> getComplaintsBySociety(Integer sid) {

        List<Complaint> complaints = new ArrayList<>();

        String sql = "SELECT * FROM complaint WHERE society_id = ? ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sid);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Complaint complaint = new Complaint();

                complaint.setId(rs.getLong("id"));
                complaint.setResidentName(rs.getString("resident_name"));
                complaint.setFlatNo(rs.getString("flat_no"));
                complaint.setSubject(rs.getString("subject"));
                complaint.setDescription(rs.getString("description"));
                complaint.setStatus(rs.getString("status"));
                complaint.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                complaints.add(complaint);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return complaints;
    }

    public void markComplaintResolved(Long id) {

        String sql = "UPDATE complaint SET status = 'RESOLVED' WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteComplaint(Long id) {

        String sql = "DELETE FROM complaint WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveComplaint(Complaint complaint) {

        String sql = """
            INSERT INTO complaint
            (society_id, resident_name, flat_no, subject, description, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, complaint.getSocietyId());
            ps.setString(2, complaint.getResidentName());
            ps.setString(3, complaint.getFlatNo());
            ps.setString(4, complaint.getSubject());
            ps.setString(5, complaint.getDescription());
            ps.setString(6, complaint.getStatus());
            ps.setTimestamp(7, Timestamp.valueOf(complaint.getCreatedAt()));

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}