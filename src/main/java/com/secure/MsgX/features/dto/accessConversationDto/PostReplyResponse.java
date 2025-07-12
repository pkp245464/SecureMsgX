package com.secure.MsgX.features.dto.accessConversationDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostReplyResponse {
    @JsonProperty("reply_id")
    private String replyId;

    @JsonProperty("status")
    private String status;
}
