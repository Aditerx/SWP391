package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "SELECT COUNT(*) FROM users u JOIN roles r ON r.role_id = u.role_id " +
            "WHERE UPPER(r.role_name) = 'MEMBER'", nativeQuery = true)
    long countMembers();
}
