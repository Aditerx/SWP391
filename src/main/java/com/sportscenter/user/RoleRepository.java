package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    @Query("select distinct r from Role r left join fetch r.permissions")
    List<Role> findAllWithPermissions();

    @Query("select distinct r from Role r left join fetch r.permissions where r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Integer id);

    Optional<Role> findByNameIgnoreCase(String name);
}

