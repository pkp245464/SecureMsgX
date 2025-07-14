package com.secure.MsgX.features.dto.apiUsageDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.secure.MsgX.core.enums.TicketType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiUsageMetricsResponse {

    @JsonProperty("http_method")
    private String httpMethod;

    @JsonProperty("api_endpoint")
    private String apiEndpoint;

    @JsonProperty("ticket_type")
    private TicketType ticketType;

    @JsonProperty("hit_count")
    private Long hitCount;
}
