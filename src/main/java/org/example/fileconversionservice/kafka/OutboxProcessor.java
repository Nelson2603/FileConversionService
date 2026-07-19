package org.example.fileconversionservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fileconversionservice.entity.OutboxMessage;
import org.example.fileconversionservice.service.OutboxService;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxService outboxService;

    private final KafkaTemplate<String, String> kafkaTemplate;
    @Scheduled(fixedDelay = 5000)
    public void processOutboxMessages() {

        List<OutboxMessage> pendingMessages = outboxService.getPendingMessages();

        if (pendingMessages.isEmpty()){
            return;
        }
        log.info("Найдено {} сообщение в Outbox для отправки ", pendingMessages.size());
        for(OutboxMessage message : pendingMessages) {
            try {
                kafkaTemplate.send(message.getTopic(), message.getMessageId(), message.getPayload());
                outboxService.markAsSend(message.getId());
                log.info("Сообщение {} отправленно в кафка ", message.getId());
            } catch (Exception e) {
                    log.info("Ошибка отпраки сообщения {} в Кафку ", message.getId());
            }
        }
    }
}
