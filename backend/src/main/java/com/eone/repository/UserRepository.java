package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id=:id")
    Optional<User> findLockedById(UUID id);
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findByDepartmentIdAndStatus(UUID departmentId, UserStatus status);
    List<User> findByDepartmentId(UUID departmentId);
}
