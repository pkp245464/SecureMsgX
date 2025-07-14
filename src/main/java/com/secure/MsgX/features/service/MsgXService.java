package com.secure.MsgX.features.service;

import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.features.dto.accessConversationDto.PostReplyRequest;
import com.secure.MsgX.features.dto.accessConversationDto.PostReplyResponse;
import com.secure.MsgX.features.dto.apiUsageDto.ApiUsageMetricsResponse;
import com.secure.MsgX.features.dto.commonDto.UnifiedViewRequest;
import com.secure.MsgX.features.dto.accessConversationDto.ViewConversationResponse;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface MsgXService {
    TicketCreationResponse createSecureTicket(TicketCreationRequest ticketCreationRequest, HttpServletRequest httpServletRequest);
    String permanentlyDeleteTicket(String ticketId);
    Object viewUnifiedTicket(UnifiedViewRequest request, String clientIp);
    PostReplyResponse postReply(PostReplyRequest request, String clientIp);
    public List<ApiUsageMetricsResponse> getApiUsageMetrics();
}
