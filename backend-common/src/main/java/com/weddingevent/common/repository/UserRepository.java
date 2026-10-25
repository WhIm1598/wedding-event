package com.weddingevent.common.repository;

import com.weddingevent.common.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = "staffMember")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByGoogleSub(String googleSub);

    /** Accounts changing their e-mail to this address (FN-AUTH-04) */
    List<User> findByPendingEmailIgnoreCase(String pendingEmail);
}
