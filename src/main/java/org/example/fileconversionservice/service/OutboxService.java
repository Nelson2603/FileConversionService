package org.example.fileconversionservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fileconversionservice.entity.OutboxMessage;
import org.example.fileconversionservice.entity.OutboxStatus;
import org.example.fileconversionservice.repository.OutboxRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    @Transactional
    public void saveMessage(String messageId,String payload,String topic){
        OutboxMessage outboxMessage = new OutboxMessage(messageId,payload,topic);
        outboxRepository.save(outboxMessage);
        log.info("Сообщение сохранено в Outbox: messageId={}", messageId);
    }

    @Transactional
    public void markAsSend(Long id){
       OutboxMessage message = outboxRepository.findById(id).orElseThrow();
       message.setStatus(OutboxStatus.SENT);
       message.setProcessedAt(LocalDateTime.now());
       outboxRepository.save(message);
       log.info("Сообщение отмечено отправленным: messageId={}", message.getId());

    }
    public List<OutboxMessage> getPendingMessages(){
        return outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    @Transactional
    public void incrementRetryCount(Long id, String errorMessage) {
        OutboxMessage message = outboxRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outbox message not found: " + id));
        message.setRetryCount(message.getRetryCount() + 1);
        message.setLastError(errorMessage);
        outboxRepository.save(message);
    }

    @Transactional
    public void markAsFailed(Long id, String errorMessage) {
        OutboxMessage message = outboxRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outbox message not found: " + id));
        message.setStatus(OutboxStatus.FAILED);
        message.setLastError(errorMessage);
        outboxRepository.save(message);
    }
}
