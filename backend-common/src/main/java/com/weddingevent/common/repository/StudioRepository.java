package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Studio;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioRepository extends JpaRepository<Studio, UUID> {}
