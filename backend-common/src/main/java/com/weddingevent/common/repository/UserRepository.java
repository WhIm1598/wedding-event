package com.weddingevent.common.repository;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = "staffMember")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    /** Row lock for the admin login, so concurrent wrong attempts all count (FN-ADM-AUTH-01 step 3) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    Optional<User> findByGoogleSub(String googleSub);

    /** Accounts changing their e-mail to this address (FN-AUTH-04) */
    List<User> findByPendingEmailIgnoreCase(String pendingEmail);

    /** Row lock that serializes one user's concurrent writes, e.g. two quote requests for the same package (FN-VND-01) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
