CREATE TABLE IF NOT EXISTS tenant (
                                      tenant_id SERIAL PRIMARY KEY,
                                      name VARCHAR(255) NOT NULL,
    contact_no VARCHAR(50) NOT NULL
    );

ALTER TABLE resident ADD COLUMN IF NOT EXISTS is_tenant BOOLEAN DEFAULT FALSE;
ALTER TABLE resident ADD COLUMN IF NOT EXISTS tenant_id INTEGER REFERENCES tenant(tenant_id);
