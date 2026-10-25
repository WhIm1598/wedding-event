-- FN-ADM-CONTR-02 (v1.1) "Xác nhận bàn giao": a contract completes only when paid in full AND delivered (§1.6).
ALTER TABLE contracts ADD COLUMN delivered_at TIMESTAMPTZ;
