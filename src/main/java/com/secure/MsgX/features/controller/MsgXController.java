package com.secure.MsgX.features.controller;

import com.secure.MsgX.features.dto.accessConversationDto.PostReplyRequest;
import com.secure.MsgX.features.dto.accessConversationDto.PostReplyResponse;
import com.secure.MsgX.features.dto.accessConversationDto.ViewConversationRequest;
import com.secure.MsgX.features.dto.accessConversationDto.ViewConversationResponse;
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


    // NOTE: just added for testing purpose, I will be removed after testing
    @PostMapping("/view-conversation")
    public ResponseEntity<ViewConversationResponse> viewConversation(@RequestBody ViewConversationRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        ViewConversationResponse response = msgXService.viewConversation(request, clientIp);
        return ResponseEntity.ok(response);
    }

    // NOTE: just added for testing purpose, I will be removed after testing
    @PostMapping("/post-reply")
    public ResponseEntity<PostReplyResponse> postReply(@RequestBody PostReplyRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        PostReplyResponse response = msgXService.postReply(request, clientIp);
        return ResponseEntity.ok(response);
    }
}
