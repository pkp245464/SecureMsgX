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

    // TODO: Enforce passkey creation limits and implement premium token system for extended access
    /**
     * Passkey Creation Policy:

     * By default, users are allowed to create a maximum of 3 passkeys when creating a secure ticket.
     * The system can support up to 10 passkeys per ticket, but allowing that for everyone increases infrastructure costs,
     * especially due to encryption complexity.

     * Premium Access via Limited-Use Token:
     * - Users who want to create more than 3 passkeys must purchase a "Premium Passkey Token".
     * - This token allows creation of tickets with up to 27 passkeys.
     * - Each token is valid for a limited number of ticket creations (e.g., 27 uses).
     * - After the limit is exhausted, users must purchase a new token.

     * Implementation Plan:
     * 1. Enforce a default limit of 3 passkeys during ticket creation.
     * 2. Build an API to allow users to purchase a premium token (`/purchase-premium-token`).
     * 3. Track token usage and store the remaining quota (e.g., remainingUses = 10).
     * 4. When a user submits a ticket with >3 passkeys:
     *    - Require a valid premium token.
     *    - Deduct one use from the token.
     * 5. Optional: Link the token to a user ID or client IP for better control.
     * 6. Log all premium token usage for billing and auditing.
     */


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
