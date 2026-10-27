package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "role")
    java.util.List<User> findAll();
    @Query("select distinct u from User u " +
            "left join fetch u.role r " +
            "left join fetch r.permissions " +
            "where lower(u.email) = lower(:email)")
    Optional<User> findForAuthentication(@Param("email") String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) FROM users u JOIN roles r ON r.role_id = u.role_id " +
            "WHERE UPPER(r.role_name) = 'MEMBER'", nativeQuery = true)
    long countMembers();

    @Query(value = "SELECT u.* FROM users u JOIN roles r ON r.role_id=u.role_id " +
            "WHERE UPPER(r.role_name)='RECEPTIONIST' AND LOWER(u.status)='active' " +
            "AND (:centerId IS NULL OR u.center_id=:centerId)", nativeQuery = true)
    java.util.List<User> findActiveReceptionists(@Param("centerId") Integer centerId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id=:id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);
}
