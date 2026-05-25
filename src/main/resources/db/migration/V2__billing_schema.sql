-- ============================================================================
-- BILLING SCHEMA MIGRATION
-- Run this script against your PostgreSQL database to create the new tables
-- required for the occupancy-aware billing system.
-- ============================================================================

-- 1. Master charge type table (admin-configurable per society)
CREATE TABLE IF NOT EXISTS charge_type (
    id              SERIAL PRIMARY KEY,
    society_id      INTEGER NOT NULL REFERENCES society(sid),
    name            VARCHAR(100) NOT NULL,
    default_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    applicable_to   VARCHAR(10) NOT NULL DEFAULT 'ALL'
                    CHECK (applicable_to IN ('ALL', 'OWNER', 'TENANT')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- Index for fast lookup by society
CREATE INDEX IF NOT EXISTS idx_charge_type_society ON charge_type(society_id);

-- 2. Charge type history (frozen snapshot at billing time)
--    Drop and recreate if it exists with the old schema
--    WARNING: Only run the DROP if you have no production data in this table!
-- DROP TABLE IF EXISTS charge_type_history CASCADE;

CREATE TABLE IF NOT EXISTS charge_type_history (
    history_id          SERIAL PRIMARY KEY,
    charge_type_id      INTEGER NOT NULL REFERENCES charge_type(id),
    society_id          INTEGER NOT NULL REFERENCES society(sid),
    name_at_billing     VARCHAR(100) NOT NULL,
    amount_at_billing   NUMERIC(12,2) NOT NULL,
    applicable_to       VARCHAR(10) NOT NULL DEFAULT 'ALL'
                        CHECK (applicable_to IN ('ALL', 'OWNER', 'TENANT')),
    month               VARCHAR(20),
    year                INTEGER
);

CREATE INDEX IF NOT EXISTS idx_cth_society_month ON charge_type_history(society_id, month, year);

-- 3. Add new columns to unit_bill_record if they don't exist
-- (total_amount, fine_amount, paid_date)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'unit_bill_record' AND column_name = 'total_amount'
    ) THEN
        ALTER TABLE unit_bill_record ADD COLUMN total_amount NUMERIC(12,2) DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'unit_bill_record' AND column_name = 'fine_amount'
    ) THEN
        ALTER TABLE unit_bill_record ADD COLUMN fine_amount NUMERIC(12,2);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'unit_bill_record' AND column_name = 'paid_date'
    ) THEN
        ALTER TABLE unit_bill_record ADD COLUMN paid_date DATE;
    END IF;
END $$;

-- 4. Refactor bill_line_item to link to unit_bill_record instead of bill
--    Add unit_bill_record_id and amount columns if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bill_line_item' AND column_name = 'unit_bill_record_id'
    ) THEN
        ALTER TABLE bill_line_item ADD COLUMN unit_bill_record_id INTEGER REFERENCES unit_bill_record(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bill_line_item' AND column_name = 'amount'
    ) THEN
        ALTER TABLE bill_line_item ADD COLUMN amount NUMERIC(12,2);
    END IF;

    -- Add applicable_to to charge_type_history if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'charge_type_history' AND column_name = 'applicable_to'
    ) THEN
        ALTER TABLE charge_type_history ADD COLUMN applicable_to VARCHAR(10) DEFAULT 'ALL';
    END IF;

    -- Add society_id to charge_type_history if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'charge_type_history' AND column_name = 'society_id'
    ) THEN
        ALTER TABLE charge_type_history ADD COLUMN society_id INTEGER REFERENCES society(sid);
    END IF;
END $$;

-- 5. Optional: Drop the old bill_id column from bill_line_item
--    Uncomment ONLY after verifying no code references bill_line_item.bill_id
-- ALTER TABLE bill_line_item DROP COLUMN IF EXISTS bill_id;
-- ALTER TABLE bill_line_item DROP COLUMN IF EXISTS month;
-- ALTER TABLE bill_line_item DROP COLUMN IF EXISTS year;

-- ============================================================================
-- DONE. The schema now supports:
--   charge_type       → master charge definitions with applicable_to (ALL/OWNER/TENANT)
--   charge_type_history → frozen snapshots at billing time
--   unit_bill_record  → per-flat bill with total_amount, fine_amount, paid_date
--   bill_line_item    → links unit_bill_record to charge_type_history with amount
-- ============================================================================
