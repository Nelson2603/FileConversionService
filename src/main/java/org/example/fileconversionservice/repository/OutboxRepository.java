package org.example.fileconversionservice.repository;

import org.example.fileconversionservice.entity.OutboxMessage;
import org.example.fileconversionservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage,Long> {
    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
