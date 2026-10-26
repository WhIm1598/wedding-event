-- #33: hibernate.jdbc.time_zone=UTC stored bookings.event_time shifted by Asia/Ho_Chi_Minh's 1970 offset (UTC+8), so
-- every time written so far is 8 hours early (15:00 was saved as 07:00). The setting is gone; put the saved times back.
-- Assumes all rows were written by a backend running in Asia/Ho_Chi_Minh (dev, demo data); no production data yet.
UPDATE bookings SET event_time = event_time + INTERVAL '8 hours';
