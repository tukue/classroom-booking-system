CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS customers (
    customer_id UUID PRIMARY KEY,
    customer_type VARCHAR(20) NOT NULL CHECK (customer_type IN ('COMPANY', 'INDIVIDUAL')),
    display_name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    phone VARCHAR(50),
    CHECK (length(trim(display_name)) > 0),
    CHECK (length(trim(email)) > 0)
);

DO $constraints$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'customers'::regclass AND conname = 'customers_email_key'
    ) THEN
        ALTER TABLE customers ADD CONSTRAINT customers_email_key UNIQUE (email);
    END IF;
END;
$constraints$;

CREATE TABLE IF NOT EXISTS booking_users (
    booking_user_id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL CHECK (length(trim(name)) > 0),
    email VARCHAR(320) NOT NULL UNIQUE CHECK (length(trim(email)) > 0)
);

DO $constraints$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'booking_users'::regclass AND conname = 'booking_users_email_key'
    ) THEN
        ALTER TABLE booking_users ADD CONSTRAINT booking_users_email_key UNIQUE (email);
    END IF;
END;
$constraints$;

CREATE TABLE IF NOT EXISTS classrooms (
    classroom_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE CHECK (length(trim(name)) > 0),
    seating_capacity INTEGER NOT NULL CHECK (seating_capacity > 0),
    accessible BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS equipment (
    equipment_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE CHECK (length(trim(name)) > 0)
);

CREATE TABLE IF NOT EXISTS classroom_equipment (
    classroom_id UUID NOT NULL REFERENCES classrooms(classroom_id) ON DELETE CASCADE,
    equipment_id UUID NOT NULL REFERENCES equipment(equipment_id) ON DELETE RESTRICT,
    PRIMARY KEY (classroom_id, equipment_id)
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT,
    booking_user_id UUID NOT NULL REFERENCES booking_users(booking_user_id) ON DELETE RESTRICT,
    classroom_id UUID NOT NULL REFERENCES classrooms(classroom_id) ON DELETE RESTRICT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    comments TEXT,
    CHECK (starts_at < ends_at),
    EXCLUDE USING gist (
        classroom_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    )
);

CREATE INDEX IF NOT EXISTS bookings_customer_starts_at_idx
    ON bookings (customer_id, starts_at);

CREATE INDEX IF NOT EXISTS bookings_classroom_starts_at_idx
    ON bookings (classroom_id, starts_at);
