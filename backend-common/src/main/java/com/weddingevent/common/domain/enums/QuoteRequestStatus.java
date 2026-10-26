package com.weddingevent.common.domain.enums;

/** Quote Request lifecycle (spec §1.6, FN-VND-03). ACCEPTED, DECLINED and CANCELLED are final. */
public enum QuoteRequestStatus {
    PENDING, QUOTED, ACCEPTED, DECLINED, CANCELLED;

    /** Still waiting for the vendor or the couple */
    public boolean isOpen() {
        return this == PENDING || this == QUOTED;
    }
}
