package com.weddingevent.common.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Human-readable business codes, numbered per studio in {@code studio_counters} (ADR-0001, V18). */
@Component
public class CodeGenerator {

    public enum Code {
        STAFF("NV-%03d"),
        CONTRACT("HD-%03d"),
        TRANSACTION("TRX-%03d");

        private final String format;

        Code(String format) {
            this.format = format;
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    /** The upsert takes a row lock, so concurrent requests of one studio never get the same number */
    public String next(Code code, UUID studioId) {
        Number value = (Number) entityManager.createNativeQuery("""
                        insert into studio_counters (studio_id, kind, last_value) values (:studio, :kind, 1)
                        on conflict (studio_id, kind) do update set last_value = studio_counters.last_value + 1
                        returning last_value""")
                .setParameter("studio", studioId)
                .setParameter("kind", code.name())
                .getSingleResult();
        return String.format(code.format, value.longValue());
    }
}
