package com.secure.MsgX.features.dto.accessConversationDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ViewConversationResponse {

    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("decrypted_content")
    private String decryptedContent;

    @JsonProperty("open_from")
    private Instant openFrom;

    @JsonProperty("open_until")
    private Instant openUntil;

    @JsonProperty("max_views")
    private Long maxViews;

    @JsonProperty("remaining_view")
    private Long remainingViews;

    @JsonProperty("ticket_status")
    private String ticketStatus;

    @JsonProperty("read_at")
    private Instant readAt;

    @JsonProperty("replies")
    private List<ConversationNode> replies;
}
