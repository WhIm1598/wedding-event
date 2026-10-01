package com.weddingevent.common.repository;

import com.weddingevent.common.domain.ServicePackage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

    /** Studio-owned packages (vendor_id IS NULL), most expensive first */
    List<ServicePackage> findAllByVendorIdIsNullOrderByPriceDesc();
}
