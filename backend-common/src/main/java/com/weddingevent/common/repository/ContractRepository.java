package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.enums.ContractStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findAllByOrderByContractDateDescCreatedAtDesc();

    long countByStatusIn(Collection<ContractStatus> statuses);

    long countByContractDateBetween(LocalDate from, LocalDate to);

    boolean existsByLeadId(UUID leadId);

    /** Case-insensitive search on customer, phone or contract number; {@code pattern} lower-case with % */
    @Query("""
            select c from Contract c
            where lower(c.customerName) like :pattern or c.phone like :pattern or lower(c.contractNumber) like :pattern
            order by c.contractDate desc""")
    List<Contract> search(@Param("pattern") String pattern, Limit limit);
}
