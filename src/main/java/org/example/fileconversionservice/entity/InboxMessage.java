package org.example.fileconversionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_messages")
@Getter

@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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
