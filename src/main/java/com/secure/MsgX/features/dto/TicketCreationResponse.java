package com.secure.MsgX.features.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.secure.MsgX.core.enums.EncryptionAlgo;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.enums.TicketType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketCreationResponse {

    /**
     * Internal ticket ID used only by the sender.
     * Can be used to immediately revoke or manage ticket access.
     */
    @JsonProperty("ticket_id")
    private String ticketId;

    /**
     * Public ticket number shared with recipients.
     * Used to retrieve or access the ticket contents.
     */
    @JsonProperty("ticket_number")
    private String ticketNumber;


    /**
     * Access timing details based on what the user provided during ticket creation.
     * Only one of the following will be returned:
     * 1) `expiresAt` — if an absolute expiration time was configured.
     * OR
     * 2) `openFrom` and `openUntil` — if a scheduled access window was configured.
     */
    @JsonProperty("expires_at")
    private Instant expiresAt;

    @JsonProperty("open_from")
    private Instant openFrom;

    @JsonProperty("open_until")
    private Instant openUntil;


    // Security information
    @JsonProperty("encryption_algo")
    private EncryptionAlgo encryptionAlgo;

    @JsonProperty("passkey")
    private List<String>passkey;

    @JsonProperty("salt")
    private String salt;


    // Status information
    @JsonProperty("ticket_status")
    private TicketStatus ticketStatus;

    @JsonProperty("ticket_type")
    private TicketType ticketType;

    @JsonProperty("allow_replies")
    private boolean allowReplies;

    /**
     * Number of times the ticket has been viewed.
     * Increments only when accessed using the ticketNumber, not when accessed using the ticketId.
     */
    @JsonProperty("count_views")
    private Integer countViews;
}
