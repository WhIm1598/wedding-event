package com.weddingevent.common.repository;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** One studio's console accounts for FN-ADM-USER-01; a null role or active filter matches everything */
    @EntityGraph(attributePaths = "staffMember")
    @Query("""
            select u from User u where u.studioId = :studioId and u.role in :roles
              and (:role is null or u.role = :role) and (:active is null or u.active = :active)
            order by u.fullName, u.email""")
    Page<User> findConsoleUsers(@Param("studioId") UUID studioId, @Param("roles") Collection<Role> roles,
            @Param("role") Role role, @Param("active") Boolean active, Pageable pageable);

    /** Locks the studio's active admins, so two admins demoting each other at once can't leave none (LAST_ADMIN) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from User u where u.studioId = :studioId
              and u.role = com.weddingevent.common.domain.enums.Role.ROLE_ADMIN and u.active = true order by u.id""")
    List<User> findActiveAdminsForUpdate(@Param("studioId") UUID studioId);

    long countByStudioIdAndRole(UUID studioId, Role role);

    Optional<User> findByStaffMemberId(UUID staffMemberId);

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
