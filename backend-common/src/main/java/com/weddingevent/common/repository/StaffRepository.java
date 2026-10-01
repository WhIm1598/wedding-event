package com.weddingevent.common.repository;

import com.weddingevent.common.domain.StaffMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<StaffMember, UUID> {

    List<StaffMember> findAllByOrderByCodeAsc();
}
