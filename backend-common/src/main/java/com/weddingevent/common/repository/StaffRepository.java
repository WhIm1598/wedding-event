package com.weddingevent.common.repository;

import com.weddingevent.common.domain.StaffMember;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffRepository extends JpaRepository<StaffMember, UUID> {

    List<StaffMember> findAllByOrderByCodeAsc();

    /** Row locks in id order (no deadlock between overlapping staff lists) so a schedule check and its insert can't interleave */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StaffMember s where s.id in :ids order by s.id")
    List<StaffMember> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
