package com.secure.MsgX.features.dto.accessConversationDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.secure.MsgX.features.dto.accessDto.PasskeyEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ViewConversationRequest {
    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("passkeys")
    private List<PasskeyEntry> passkeys;
}
