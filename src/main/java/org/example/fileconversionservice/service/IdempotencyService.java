package org.example.fileconversionservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fileconversionservice.entity.InboxMessage;
import org.example.fileconversionservice.repository.InboxRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    private final InboxRepository inboxRepository;

    @Transactional
    public boolean tryToAcquireLock(String messageId){
        // Сначала быстрая проверка (чтобы не бросать исключения лишний раз)
        if(inboxRepository.existsByMessageId(messageId)){
            log.info("Message with id {} was already processed. Skipping.", messageId);
            return false;
        }
        try{
            inboxRepository.save(new InboxMessage(messageId));
            return true;
        } catch (DataIntegrityViolationException e){
            log.warn("Duplicate message detected via DB constraint: {}", messageId);
            return false;
        }
    }


}
