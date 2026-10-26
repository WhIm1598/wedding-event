-- FN-ADM-SET-01 / FN-SVC-02 (v1.1.3): couples contact the studio on Zalo from the app; NULL = no Zalo link.
ALTER TABLE studio_settings ADD COLUMN zalo_phone VARCHAR(20);
