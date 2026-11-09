-- A booking can now contain several services (done one after another).
CREATE TABLE booking_item (
    id            BIGSERIAL PRIMARY KEY,
    booking_id    BIGINT NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    service_id    BIGINT NOT NULL REFERENCES service_offering(id),
    service_name  VARCHAR(120) NOT NULL,
    price         NUMERIC(10,2) NOT NULL,
    duration_min  INT NOT NULL
);
CREATE INDEX idx_booking_item_booking ON booking_item(booking_id);

ALTER TABLE booking ADD COLUMN duration_min INT;
ALTER TABLE booking ADD COLUMN slot_end TIMESTAMP;
ALTER TABLE booking ALTER COLUMN service_name TYPE VARCHAR(500);

UPDATE booking b
   SET duration_min = s.duration_min,
       slot_end = b.slot_start + make_interval(mins => s.duration_min)
  FROM service_offering s
 WHERE s.id = b.service_id;

INSERT INTO booking_item (booking_id, service_id, service_name, price, duration_min)
SELECT b.id, b.service_id, b.service_name, b.price, b.duration_min FROM booking b;

ALTER TABLE booking ALTER COLUMN duration_min SET NOT NULL;
ALTER TABLE booking ALTER COLUMN slot_end SET NOT NULL;
CREATE INDEX idx_booking_merchant_end ON booking(merchant_id, slot_end);
