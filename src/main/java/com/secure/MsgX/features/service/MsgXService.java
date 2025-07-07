package com.secure.MsgX.features.service;

import com.secure.MsgX.features.dto.TicketCreationRequest;
import com.secure.MsgX.features.dto.TicketCreationResponse;

public interface MsgXService {
    TicketCreationResponse createSecureTicket(TicketCreationRequest ticketCreationRequest);
}
