package org.example.fileconversionservice.repository;

import org.example.fileconversionservice.entity.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboxRepository extends JpaRepository<InboxMessage,Long> {

    //метод для проверки, есть ли такое сообщение в бд
    boolean existsByMessageId(String messageId);

    Optional<InboxMessage> findByMessageId(String messageId);
}
