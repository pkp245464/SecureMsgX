package com.secure.MsgX.features.controller;

import com.secure.MsgX.features.dto.accessConversationDto.PostReplyRequest;
import com.secure.MsgX.features.dto.accessConversationDto.PostReplyResponse;
import com.secure.MsgX.features.dto.commonDto.UnifiedViewRequest;
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

    @PostMapping("/view")
    public ResponseEntity<?> viewTicketContent(@RequestBody UnifiedViewRequest request, HttpServletRequest httpRequest) {
        log.info("MsgXController::viewTicketContent - Received request to view ticket: {}", request.getTicketNumber());
        String clientIp = httpRequest.getRemoteAddr();
        Object response = msgXService.viewUnifiedTicket(request, clientIp);
        log.info("MsgXController::viewTicketContent - Successfully processed view for ticket: {}", request.getTicketNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/replies")
    public ResponseEntity<PostReplyResponse> replyToTicket(@RequestBody PostReplyRequest request, HttpServletRequest httpRequest) {
        log.info("MsgXController::postReply - Received request to post reply to ticket: {}", request.getTicketNumber());
        String clientIp = httpRequest.getRemoteAddr();
        PostReplyResponse response = msgXService.postReply(request, clientIp);
        log.info("MsgXController::postReply - Successfully processed reply for ticket: {}", request.getTicketNumber());
        return ResponseEntity.ok(response);
    }
}
