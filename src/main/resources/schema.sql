-- 1. Create sequences
CREATE SEQUENCE IF NOT EXISTS seq_society_id START WITH 101 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS announcement_no START WITH 1000001 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_bill_id START WITH 10001 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS complaint_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_login_id START WITH 1001 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_bill_no START WITH 100001 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS todo_no START WITH 1 INCREMENT BY 1;

-- 2. Create tables
CREATE TABLE IF NOT EXISTS admin (
    name VARCHAR(200),
    username VARCHAR(200) PRIMARY KEY,
    password VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS society (
    sid INTEGER DEFAULT NEXTVAL('seq_society_id') PRIMARY KEY,
    name VARCHAR(200),
    street VARCHAR(100),
    landmark VARCHAR(100),
    locality VARCHAR(100),
    pincode VARCHAR(10),
    city VARCHAR(50),
    state VARCHAR(100),
    country VARCHAR(100),
    admin_id INTEGER,
    data_uploaded BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS flat (
    flat_id SERIAL PRIMARY KEY,
    flat_no VARCHAR(50) NOT NULL,
    society_id INTEGER REFERENCES society(sid),
    owner_mem_id VARCHAR(50),
    occupancy_type VARCHAR(50) DEFAULT 'OWNER',
    mygate_no VARCHAR(50) UNIQUE
);

CREATE TABLE IF NOT EXISTS tenant (
    tenant_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_no VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    bill_type VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS resident (
    mem_id INTEGER PRIMARY KEY,
    sid INTEGER REFERENCES society(sid),
    name VARCHAR(200),
    room_no INTEGER,
    mr_ms VARCHAR(6),
    gender VARCHAR(20),
    age INTEGER,
    contact_no VARCHAR(20),
    isadmin BOOLEAN,
    mygate_no VARCHAR(6) UNIQUE,
    bhk VARCHAR(15),
    email VARCHAR(255),
    password VARCHAR(255),
    is_tenant BOOLEAN DEFAULT FALSE,
    tenant_id INTEGER REFERENCES tenant(tenant_id)
);

CREATE TABLE IF NOT EXISTS login (
    id INTEGER DEFAULT NEXTVAL('seq_login_id') PRIMARY KEY,
    mem_id INTEGER REFERENCES resident(mem_id),
    email_id VARCHAR(50),
    password VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS announcements (
    id INTEGER DEFAULT NEXTVAL('announcement_no') PRIMARY KEY,
    sid INTEGER NOT NULL REFERENCES society(sid),
    title VARCHAR(255) NOT NULL,
    message VARCHAR(10000) NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS complaint (
    id INTEGER DEFAULT NEXTVAL('complaint_id_seq') PRIMARY KEY,
    society_id INTEGER NOT NULL REFERENCES society(sid),
    resident_name VARCHAR(100),
    flat_no VARCHAR(20),
    subject VARCHAR(200),
    description VARCHAR(10000),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS note (
    sid INTEGER REFERENCES society(sid),
    title VARCHAR(255),
    message VARCHAR(10000)
);

CREATE TABLE IF NOT EXISTS resident_bill (
    mygate_no VARCHAR(6),
    year INTEGER CHECK (year >= 1000 AND year <= 9999),
    january SMALLINT DEFAULT 0 CHECK (january IN (0, 1, 2)),
    february SMALLINT DEFAULT 0 CHECK (february IN (0, 1, 2)),
    march SMALLINT DEFAULT 0 CHECK (march IN (0, 1, 2)),
    april SMALLINT DEFAULT 0 CHECK (april IN (0, 1, 2)),
    may SMALLINT DEFAULT 0 CHECK (may IN (0, 1, 2)),
    june SMALLINT DEFAULT 0 CHECK (june IN (0, 1, 2)),
    july SMALLINT DEFAULT 0 CHECK (july IN (0, 1, 2)),
    august SMALLINT DEFAULT 0 CHECK (august IN (0, 1, 2)),
    september SMALLINT DEFAULT 0 CHECK (september IN (0, 1, 2)),
    october SMALLINT DEFAULT 0 CHECK (october IN (0, 1, 2)),
    november SMALLINT DEFAULT 0 CHECK (november IN (0, 1, 2)),
    december SMALLINT DEFAULT 0 CHECK (december IN (0, 1, 2)),
    CONSTRAINT unique_mygate_year UNIQUE (mygate_no, year)
);

CREATE TABLE IF NOT EXISTS flat_tenancy (
    tenancy_id SERIAL PRIMARY KEY,
    flat_id INTEGER REFERENCES flat(flat_id),
    tenant_id INTEGER REFERENCES tenant(tenant_id),
    lease_start_date DATE NOT NULL,
    lease_end_date DATE,
    security_deposit DECIMAL(12,2),
    rent_amount DECIMAL(12,2)
);

CREATE TABLE IF NOT EXISTS charge_type (
    id SERIAL PRIMARY KEY,
    society_id INTEGER REFERENCES society(sid),
    name VARCHAR(255),
    default_amount DECIMAL(10,2),
    applicable_to VARCHAR(50) DEFAULT 'ALL' CHECK (applicable_to IN ('ALL', 'OWNER', 'TENANT')),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS charge_type_history (
    history_id SERIAL PRIMARY KEY,
    charge_type_id INTEGER REFERENCES charge_type(id),
    society_id INTEGER REFERENCES society(sid),
    name_at_billing VARCHAR(255),
    amount_at_billing DECIMAL(10,2),
    applicable_to VARCHAR(50) DEFAULT 'ALL' CHECK (applicable_to IN ('ALL', 'OWNER', 'TENANT')),
    month VARCHAR(20),
    year INTEGER
);

-- Note: We drop and recreate bill if needed, but IF NOT EXISTS is fine.
CREATE TABLE IF NOT EXISTS bill (
    id SERIAL PRIMARY KEY,
    sid INTEGER REFERENCES society(sid),
    due_date DATE,
    month VARCHAR(20),
    year INTEGER,
    maintenance_contribution INTEGER DEFAULT 0,
    housing_board_contribution INTEGER DEFAULT 0,
    property_tax_contribution INTEGER DEFAULT 0,
    sinking_fund INTEGER DEFAULT 0,
    reserve_mhada_service_charge INTEGER DEFAULT 0,
    sub_charge INTEGER DEFAULT 0,
    fine INTEGER DEFAULT 0,
    building_dev_fund INTEGER DEFAULT 0,
    other INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS unit_bill_record (
    id SERIAL PRIMARY KEY,
    bill_id INTEGER REFERENCES bill(id),
    flat_id INTEGER REFERENCES flat(flat_id),
    status VARCHAR(50),
    total_amount DECIMAL(10,2),
    fine_amount DECIMAL(10,2),
    paid_date DATE,
    month VARCHAR(20),
    year INTEGER
);

CREATE TABLE IF NOT EXISTS bill_line_item (
    id SERIAL PRIMARY KEY,
    unit_bill_record_id INTEGER REFERENCES unit_bill_record(id),
    charge_type_history_id INTEGER REFERENCES charge_type_history(history_id),
    amount DECIMAL(10,2)
);

-- 3. Schema Evolution (Safe ALTER TABLE statements for existing databases)
ALTER TABLE IF EXISTS flat ADD COLUMN IF NOT EXISTS occupancy_type VARCHAR(50) DEFAULT 'OWNER';
ALTER TABLE IF EXISTS resident ADD COLUMN IF NOT EXISTS is_tenant BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS resident ADD COLUMN IF NOT EXISTS tenant_id INTEGER REFERENCES tenant(tenant_id);
-- Bill contribution columns (added for existing deployments)
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS maintenance_contribution INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS housing_board_contribution INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS property_tax_contribution INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS sinking_fund INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS reserve_mhada_service_charge INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS sub_charge INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS fine INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS building_dev_fund INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS bill ADD COLUMN IF NOT EXISTS other INTEGER DEFAULT 0;
