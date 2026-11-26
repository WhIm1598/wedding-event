-- Studio logo (small PNG/JPEG, <= 200 KB), carried in JSON as a data URL
ALTER TABLE studio_settings ADD COLUMN logo BYTEA, ADD COLUMN logo_content_type VARCHAR(20);
