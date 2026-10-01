package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "staff_tasks")
public class StaffTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_member_id", nullable = false)
    private StaffMember staffMember;

    @Column(nullable = false)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    private String notes;

    @Column(nullable = false)
    private boolean completed;
}
