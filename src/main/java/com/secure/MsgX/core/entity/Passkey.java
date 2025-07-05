package com.secure.MsgX.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "passkey")
public class Passkey {

    @Id
    @Column(name = "passkey_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String passkeyId;

    @Column(name = "passkey_hash")
    private String passkeyHash;

    @Column(name = "key_order")
    private Integer keyOrder; // Validation: 1-10

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
}
