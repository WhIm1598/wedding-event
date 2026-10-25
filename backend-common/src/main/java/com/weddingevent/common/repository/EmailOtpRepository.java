package com.weddingevent.common.repository;

import com.weddingevent.common.domain.EmailOtp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findByUserId(UUID userId);

    /** Codes sent to this address; several accounts may be changing to the same (pending) address */
    List<EmailOtp> findByAddressIgnoreCase(String address);
}
