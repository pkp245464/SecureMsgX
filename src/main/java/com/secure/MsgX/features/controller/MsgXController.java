package com.secure.MsgX.features.controller;

import com.secure.MsgX.features.dto.accessDto.ViewTicketRequest;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import com.secure.MsgX.features.service.MsgXService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/doors-of-durin/sigil-scrolls")
public class MsgXController {

    private final MsgXService msgXService;

    @PostMapping("/new-ticket")
    public ResponseEntity<TicketCreationResponse> createTicket(@RequestBody TicketCreationRequest ticketCreationRequest,
                                                               HttpServletRequest httpServletRequest) {
        log.info("MsgXController::createTicket - Received ticket creation request for userId: {}",ticketCreationRequest);
        TicketCreationResponse response = msgXService.createSecureTicket(ticketCreationRequest, httpServletRequest);
        log.info("MsgXController::createTicket - Ticket created successfully with ticketId: {}", response.getTicketId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{ticketId}")
    public ResponseEntity<String> deleteTicket(@PathVariable String ticketId) {
        log.info("MsgXController::deleteTicket - Received request to permanently delete ticketId: {}", ticketId);
        String result = msgXService.permanentlyDeleteTicket(ticketId);
        log.info("MsgXController::deleteTicket - Result for ticketId {}: {}", ticketId, result);
        return ResponseEntity.ok(result);
    }

    // NOTE: Currently handles SINGLE, SECURE_SINGLE, and BROADCAST ticket types.
    // TODO: Extend this endpoint to handle THREAD and GROUP ticket types as well.
    @PostMapping("/view-ticket")
    public ResponseEntity<ViewTicketResponse> viewTicket(@RequestBody ViewTicketRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        log.info("MsgXController::viewTicket - Viewing ticket {} from IP {}", request.getTicketNumber(), clientIp);
        ViewTicketResponse response = msgXService.viewTicket(request, clientIp);
        log.info("MsgXController::viewTicket - Ticket {} viewed successfully", request.getTicketNumber());
        return ResponseEntity.ok(response);
    }
}
