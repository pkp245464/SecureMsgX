package com.secure.MsgX.features.service;

import com.secure.MsgX.features.dto.accessConversationDto.PostReplyRequest;
import com.secure.MsgX.features.dto.accessConversationDto.PostReplyResponse;
import com.secure.MsgX.features.dto.accessConversationDto.ViewConversationRequest;
import com.secure.MsgX.features.dto.accessConversationDto.ViewConversationResponse;
import com.secure.MsgX.features.dto.accessDto.ViewTicketRequest;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface MsgXService {
    TicketCreationResponse createSecureTicket(TicketCreationRequest ticketCreationRequest, HttpServletRequest httpServletRequest);
    String permanentlyDeleteTicket(String ticketId);
    ViewTicketResponse viewTicket(ViewTicketRequest request, String clientIp);
    ViewConversationResponse viewConversation(ViewConversationRequest request, String clientIp);
    PostReplyResponse postReply(PostReplyRequest request, String clientIp);
}
