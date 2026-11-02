package com.recallradar.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndEnabledTrue(String role);

    long countByEnabled(boolean enabled);

    long countByRoleIgnoreCase(String role);

    long countByAccountStatusIgnoreCase(String accountStatus);

    List<AppUser> findByAccountStatusIgnoreCaseOrderByCreatedAtDesc(String accountStatus);

    Page<AppUser> findByAccountStatusIgnoreCaseOrderByCreatedAtDesc(String accountStatus, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE " +
           "(:role IS NULL OR :role = '' OR UPPER(u.role) = UPPER(:role)) AND " +
           "(:status IS NULL OR u.enabled = :status) AND " +
           "(:accountStatus IS NULL OR :accountStatus = '' OR UPPER(u.accountStatus) = UPPER(:accountStatus)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<AppUser> searchUsers(
            @Param("query") String query,
            @Param("role") String role,
            @Param("status") Boolean status,
            @Param("accountStatus") String accountStatus,
            Pageable pageable
    );
}
