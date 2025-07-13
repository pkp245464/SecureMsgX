package com.secure.MsgX.features.dto.accessConversationDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.secure.MsgX.features.dto.commonDto.PasskeyEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostReplyRequest {

    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("passkeys")
    private List<PasskeyEntry> passkeys;

    @JsonProperty("content")
    private String content;

    @JsonProperty("parent_reply_id")
    private String parentReplyId;
}
