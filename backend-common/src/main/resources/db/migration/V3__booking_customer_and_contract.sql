-- FN-ADM-CRM-03 / FN-ADM-CAL-02 (v1.1): a booking belongs to a customer (lead) and, for the sessions of a sold
-- service, to a contract. One contract has many bookings.
ALTER TABLE bookings ADD COLUMN lead_id UUID REFERENCES leads (id);
ALTER TABLE bookings ADD COLUMN contract_id UUID REFERENCES contracts (id);
CREATE INDEX idx_bookings_contract ON bookings (contract_id);
CREATE INDEX idx_bookings_lead ON bookings (lead_id);

-- contracts.booking_id (one booking per contract) is replaced by bookings.contract_id. The code no longer reads it;
-- drop it in a later release once no deployment of the previous version is left (db/README.md, backward compatibility).
