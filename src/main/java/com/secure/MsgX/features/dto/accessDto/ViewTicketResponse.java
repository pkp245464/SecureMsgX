package com.secure.MsgX.features.dto.accessDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ViewTicketResponse {
    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("decrypted_content")
    private String decryptedContent;

    @JsonProperty("open_from")
    private Instant openFrom;

    @JsonProperty("open_until")
    private Instant openUntil;

    @JsonProperty("max_views")
    private Integer maxViews;

    @JsonProperty("remaining_views")
    private Integer remainingViews;

    @JsonProperty("ticket_status")
    private String ticketStatus;

    @JsonProperty("read_at")
    private Instant readAt;

    @JsonProperty("security_warning")
    private String securityMessage;
}
