package com.weddingevent.common.repository;

import com.weddingevent.common.domain.StaffTask;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffTaskRepository extends JpaRepository<StaffTask, UUID> {

    List<StaffTask> findByStaffMemberIdOrderByDueDateAscCreatedAtAsc(UUID staffMemberId);
}
