package com.secure.MsgX.features.dto.accessConversationDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConversationNode {

    @JsonProperty("reply_id")
    private String replyId;

    @JsonProperty("decrypted_content")
    private String decryptedContent;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("replies")
    private List<ConversationNode> replies = new ArrayList<>();
}
