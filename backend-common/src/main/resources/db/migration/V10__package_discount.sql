-- FN-VND-05 (v1.1.3): a vendor may discount its package; NULL = no discount. Shown in the catalog (FN-SVC-01/02).
ALTER TABLE service_packages ADD COLUMN discount_price NUMERIC(15, 0)
    CHECK (discount_price > 0 AND discount_price < price);
