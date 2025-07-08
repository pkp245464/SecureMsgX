package com.secure.MsgX.features.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.secure.MsgX.core.enums.EncryptionAlgo;
import com.secure.MsgX.core.enums.TicketType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class TicketCreationRequest {

    // Message content (will be encrypted)
    @JsonProperty("message_content")
    private String messageContent;


    // Security parameters
    @JsonProperty("encryption_algo")
    private EncryptionAlgo encryptionAlgo;

    @JsonProperty("passkeys")
    private List<String> passkeys;  // Ordered passkeys (1-10)

    @JsonProperty("salt")
    private String salt;            // Optional additional salt


    // Access control
    /**
     * Expiration and access window configuration (choose one approach):
     * 1) Set `expiresAt` to specify an absolute expiration time from ticket creation.
     * OR
     * 2) Set both `openFrom` and `openUntil` to define a scheduled access window.
     * Do NOT provide both `expiresAt` and `openFrom`/`openUntil` together.
     */
    @JsonProperty("expires_at")
    private Instant expiresAt;

    @JsonProperty("open_from")
    private Instant openFrom;

    @JsonProperty("open_until")
    private Instant openUntil;

    @JsonProperty("max_views")
    private Integer maxViews;


    // Ticket configuration
    @JsonProperty("ticket_type")
    private TicketType ticketType;

    @JsonProperty("allow_replies")
    private boolean allowReplies;
}
