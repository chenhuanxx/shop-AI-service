package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {
    List<MessageEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
