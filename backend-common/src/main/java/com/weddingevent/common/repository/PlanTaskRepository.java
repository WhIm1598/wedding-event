package com.weddingevent.common.repository;

import com.weddingevent.common.domain.PlanTask;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanTaskRepository extends JpaRepository<PlanTask, UUID> {}