package org.example.fileconversionservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_messages")
@Getter
@Setter
@NoArgsConstructor
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String messageId;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column()
    LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    public OutboxMessage(String messageId, String payload, String topic) {
        this.messageId = messageId;
        this.payload = payload;
        this.topic = topic;
        this.createdAt = LocalDateTime.now();
        this.status = OutboxStatus.PENDING;
    }
}
