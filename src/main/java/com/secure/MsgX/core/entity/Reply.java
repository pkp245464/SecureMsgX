package com.secure.MsgX.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "reply")
public class Reply {

    @Id
    @Column(name = "reply_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String replyId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "encrypted_content")
    private byte[] encryptedContent;

    @Column(name = "reply_ip_address")
    private String replyIpAddress;

    @Lob
    @Column(name = "initialization_vector", nullable = false)
    private byte[] iv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
}
