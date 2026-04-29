package com.pythonadmin.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByPhone(String phone);
    Optional<UserEntity> findFirstByPhoneEndingWith(String phone);
    Page<UserEntity> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    List<UserEntity> findByUsernameContainingIgnoreCase(String username, Sort sort);
}
