package com.secure.MsgX.features.dto.accessConversationDto;

import com.secure.MsgX.features.dto.accessDto.PasskeyEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostReplyRequest {

    private String ticketNumber;
    private List<PasskeyEntry> passkeys;
    private String content;
    private String parentReplyId;
}
