package com.brijesh.authservice.repository;

import com.brijesh.authservice.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    boolean existsByEmail(String email);

//     Fetch user with roles and permissions in one query
//     Avoids N+1 problem
    @Query("""
SELECT u FROM User u
LEFT JOIN FETCH u.roles r
LEFT JOIN FETCH r.permissions
WHERE u.email = :email
""")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("""
SELECT u FROM User u
LEFT JOIN FETCH u.roles r
LEFT JOIN FETCH r.permissions
WHERE u.uuid = :uuid
""")
    Optional<User> findByUuidWithRoles(@Param("uuid") String uuid);
}
