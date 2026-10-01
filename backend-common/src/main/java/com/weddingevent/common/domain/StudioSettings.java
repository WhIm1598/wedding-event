package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Single-row studio profile printed on contracts (party B, bank details). */
@Getter
@Setter
@Entity
@Table(name = "studio_settings")
public class StudioSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private short id = SINGLETON_ID;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name = "tax_code", nullable = false)
    private String taxCode;

    @Column(name = "legal_representative", nullable = false)
    private String legalRepresentative;

    @Column(name = "bank_info", nullable = false)
    private String bankInfo;
}
