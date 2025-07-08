package com.secure.MsgX.features.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasskeyDto {

    @JsonProperty("passkey_hash")
    private String passkeyHash;

    @JsonProperty("key_order")
    private Integer keyOrder;
}
