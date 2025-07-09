package com.secure.MsgX.features.service;

import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface MsgXService {
    TicketCreationResponse createSecureTicket(TicketCreationRequest ticketCreationRequest, HttpServletRequest httpServletRequest);
}
