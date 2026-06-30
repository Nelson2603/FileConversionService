package org.example.fileconversionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String messageId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public InboxMessage(String messageId) {
        this.messageId = messageId;
        this.processedAt = LocalDateTime.now();
    }
}
