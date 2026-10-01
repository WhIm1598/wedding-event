package com.weddingevent.common.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Human-readable business codes backed by PostgreSQL sequences (see V1__init.sql). */
@Component
public class CodeGenerator {

    /** Sequence names are fixed here, never taken from user input. */
    public enum Code {
        STAFF("staff_code_seq", "NV-%03d"),
        CONTRACT("contract_number_seq", "HD-%d"),
        TRANSACTION("transaction_code_seq", "TRX-%03d");

        private final String sequence;
        private final String format;

        Code(String sequence, String format) {
            this.sequence = sequence;
            this.format = format;
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    public String next(Code code) {
        Number value = (Number) entityManager
                .createNativeQuery("select nextval('" + code.sequence + "')")
                .getSingleResult();
        return String.format(code.format, value.longValue());
    }
}
