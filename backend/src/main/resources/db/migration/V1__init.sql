-- ServiceBay initial schema

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    firebase_uid  VARCHAR(128) NOT NULL UNIQUE,
    role          VARCHAR(16),               -- CUSTOMER | MERCHANT (null until chosen)
    name          VARCHAR(120),
    phone         VARCHAR(20),
    email         VARCHAR(200),
    address       VARCHAR(500),
    pincode       VARCHAR(10),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE device_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE merchant (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    shop_name       VARCHAR(150),
    phone           VARCHAR(20),
    address         VARCHAR(500),
    pincode         VARCHAR(10),
    city            VARCHAR(80),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    serves_bike     BOOLEAN NOT NULL DEFAULT TRUE,
    serves_car      BOOLEAN NOT NULL DEFAULT FALSE,
    open_days       VARCHAR(40) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT',
    open_time       TIME NOT NULL DEFAULT '09:00',
    close_time      TIME NOT NULL DEFAULT '20:00',
    slot_minutes    INT NOT NULL DEFAULT 30,
    online          BOOLEAN NOT NULL DEFAULT TRUE,
    onboarded       BOOLEAN NOT NULL DEFAULT FALSE,
    rating_avg      NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_merchant_pincode ON merchant(pincode) WHERE onboarded;

CREATE TABLE merchant_photo (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  BIGINT NOT NULL REFERENCES merchant(id) ON DELETE CASCADE,
    url          VARCHAR(600) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0
);

CREATE TABLE service_offering (
    id            BIGSERIAL PRIMARY KEY,
    merchant_id   BIGINT NOT NULL REFERENCES merchant(id) ON DELETE CASCADE,
    category      VARCHAR(24) NOT NULL,      -- WATER_WASH | CLEANING | FULL_SERVICE | PUNCTURE | TYRE | OIL_CHANGE | BATTERY
    vehicle_type  VARCHAR(8)  NOT NULL,      -- BIKE | CAR
    name          VARCHAR(120) NOT NULL,
    description   VARCHAR(500),
    price         NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    duration_min  INT NOT NULL CHECK (duration_min > 0),
    image_url     VARCHAR(600),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_service_merchant ON service_offering(merchant_id);

CREATE TABLE booking (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(16) NOT NULL UNIQUE,
    customer_id       BIGINT NOT NULL REFERENCES app_user(id),
    merchant_id       BIGINT NOT NULL REFERENCES merchant(id),
    service_id        BIGINT NOT NULL REFERENCES service_offering(id),
    service_name      VARCHAR(120) NOT NULL,
    vehicle_type      VARCHAR(8) NOT NULL,
    price             NUMERIC(10,2) NOT NULL,
    slot_start        TIMESTAMP NOT NULL,        -- shop local time
    status            VARCHAR(16) NOT NULL,      -- REQUESTED | ACCEPTED | DECLINED | IN_PROGRESS | COMPLETED | CANCELLED
    otp               VARCHAR(6) NOT NULL,
    otp_attempts      INT NOT NULL DEFAULT 0,
    customer_name     VARCHAR(120),
    customer_phone    VARCHAR(20) NOT NULL,
    customer_address  VARCHAR(500) NOT NULL,
    customer_pincode  VARCHAR(10) NOT NULL,
    reminder_sent     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at       TIMESTAMPTZ,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ
);
CREATE INDEX idx_booking_customer ON booking(customer_id, slot_start DESC);
CREATE INDEX idx_booking_merchant ON booking(merchant_id, slot_start);
-- One active booking per shop per slot: prevents double booking even under concurrent requests.
CREATE UNIQUE INDEX uq_booking_active_slot ON booking(merchant_id, slot_start)
    WHERE status IN ('REQUESTED', 'ACCEPTED', 'IN_PROGRESS');

CREATE TABLE review (
    id           BIGSERIAL PRIMARY KEY,
    booking_id   BIGINT NOT NULL UNIQUE REFERENCES booking(id),
    merchant_id  BIGINT NOT NULL REFERENCES merchant(id),
    customer_id  BIGINT NOT NULL REFERENCES app_user(id),
    rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      VARCHAR(1000),
    reply        VARCHAR(1000),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    replied_at   TIMESTAMPTZ
);
CREATE INDEX idx_review_merchant ON review(merchant_id, created_at DESC);

CREATE TABLE notification (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    type        VARCHAR(32) NOT NULL,
    title       VARCHAR(150) NOT NULL,
    body        VARCHAR(500) NOT NULL,
    booking_id  BIGINT REFERENCES booking(id),
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_user ON notification(user_id, created_at DESC);
