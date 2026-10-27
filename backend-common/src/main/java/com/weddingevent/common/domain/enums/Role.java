package com.weddingevent.common.domain.enums;

public enum Role {
    ROLE_COUPLE, ROLE_VENDOR, ROLE_STAFF, ROLE_ADMIN,
    /** Owner of the platform (ADR-0001): manages studios, belongs to none */
    ROLE_PLATFORM_ADMIN
}

