package com.secure.MsgX.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "read_log")
public class ReadLog {

    @Id
    @Column(name = "read_log_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String readLogId;

    @CreationTimestamp
    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "read_by_ip_address", columnDefinition = "INET")
    private String readByIpAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
}
