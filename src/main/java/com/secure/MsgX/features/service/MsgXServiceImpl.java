package com.secure.MsgX.features.service;

import com.secure.MsgX.features.dto.TicketCreationRequest;
import com.secure.MsgX.features.dto.TicketCreationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgXServiceImpl implements MsgXService{

    @Override
    public TicketCreationResponse createSecureTicket(TicketCreationRequest ticketCreationRequest) {
        return null;
    }
}
