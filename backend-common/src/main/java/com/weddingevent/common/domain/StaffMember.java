package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.StaffWorkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Photographer, videographer, makeup artist or sales consultant (feature spec §5.6). */
@Getter
@Setter
@Entity
@Table(name = "staff_members")
public class StaffMember extends BaseEntity {

    /** Human-readable code, e.g. NV-001 */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false)
    private StaffWorkStatus workStatus = StaffWorkStatus.WORKING;
}
