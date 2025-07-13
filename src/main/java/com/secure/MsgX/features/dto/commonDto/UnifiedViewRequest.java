package com.secure.MsgX.features.dto.commonDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UnifiedViewRequest {
    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("passkeys")
    private List<PasskeyEntry> passkeys;
}
