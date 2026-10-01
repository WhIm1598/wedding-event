package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Lead;
import com.weddingevent.common.domain.enums.LeadStage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    List<Lead> findAllByOrderByCreatedAtDesc();

    Optional<Lead> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    long countByStageIn(Collection<LeadStage> stages);

    long countByStageInAndCreatedAtBetween(Collection<LeadStage> stages, Instant from, Instant to);

    /** Case-insensitive search on name or phone; {@code pattern} must include % wildcards and be lower-case */
    @Query("select l from Lead l where lower(l.name) like :pattern or l.phone like :pattern order by l.createdAt desc")
    List<Lead> search(@Param("pattern") String pattern, Limit limit);
}
