package com.weddingevent.common.repository;

import com.weddingevent.common.domain.VendorProject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** FN-VND-03..05. Every read is scoped to the project's vendor or couple. */
public interface VendorProjectRepository extends JpaRepository<VendorProject, UUID> {
}
