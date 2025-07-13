package com.secure.MsgX.features.dto.commonDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasskeyEntry {
    @JsonProperty("order")
    private Integer order;

    @JsonProperty("value")
    private String value;
}
