package com.secure.MsgX.features.controller;

import com.secure.MsgX.features.dto.TicketCreationRequest;
import com.secure.MsgX.features.dto.TicketCreationResponse;
import com.secure.MsgX.features.service.MsgXService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/doors-of-durin/sigil-scrolls")
public class MsgXController {

    private final MsgXService msgXService;

    @PostMapping("/new-ticket")
    public ResponseEntity<TicketCreationResponse> createTicket(@RequestBody TicketCreationRequest ticketCreationRequest) {
        log.info("MsgXController::createTicket - Received ticket creation request for userId: {}",ticketCreationRequest);
        TicketCreationResponse response = msgXService.createSecureTicket(ticketCreationRequest);
        log.info("MsgXController::createTicket - Ticket created successfully with ticketId: {}", response.getTicketId());
        return ResponseEntity.ok(response);
    }

}
