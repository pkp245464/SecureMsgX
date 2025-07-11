package com.secure.MsgX.core.entity;

import com.secure.MsgX.core.enums.EncryptionAlgo;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.enums.TicketType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ticket")
public class Ticket {

    /**
     * Internal ticket ID used only by the sender.
     * Can be used to immediately revoke or manage ticket access.
     */
    @Id
    @Column(name = "ticket_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String ticketId;

    /**
     * Public ticket number shared with recipients.
     * Used to retrieve or access the ticket contents.
     */
    @Column(name = "ticket_number")
    private String ticketNumber;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "open_from")
    private Instant openFrom;

    @Column(name = "open_until")
    private Instant openUntil;

    @Column(name = "creator_ip_address")
    private String creatorIpAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status")
    private TicketStatus ticketStatus = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "encryption_algo")
    private EncryptionAlgo encryptionAlgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type")
    private TicketType ticketType;

    @Column(name = "max_views")
    private Integer maxViews;

    @Column(name = "count_views")
    private Integer countViews = 0;

    @Column(name = "encrypted_message", columnDefinition = "TEXT")
    private String encryptedMessage;

    @Column(name = "salt")
    private String salt;

    @Column(name = "allow_replies")
    private boolean allowReplies;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Passkey> passkeys;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reply> replies;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadLog> readLogs;

    @Column(name = "initialization_vector", columnDefinition = "TEXT")
    private String iv; // IV = Initialization Vector

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ticket_id")
    private Ticket parentTicket;
}
