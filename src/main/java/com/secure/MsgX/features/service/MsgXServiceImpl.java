package com.secure.MsgX.features.service;

import com.secure.MsgX.core.entity.Passkey;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.ticketCreateDto.PasskeyDto;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import com.secure.MsgX.features.repository.PasskeyRepository;
import com.secure.MsgX.features.repository.TicketRepository;
import com.secure.MsgX.features.utility.ticketCreateUtil.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgXServiceImpl implements MsgXService{

    private final TicketBuilderService ticketBuilderService;
    private final TicketRepository ticketRepository;
    private final TicketCreationRequestValidator ticketCreationRequestValidator;

    @Override
    public TicketCreationResponse createSecureTicket(TicketCreationRequest ticketCreationRequest, HttpServletRequest httpServletRequest) {
        log.info("MsgXServiceImpl::createSecureTicket - Validating ticket request");
        ticketCreationRequestValidator.validateRequest(ticketCreationRequest);
        log.info("MsgXServiceImpl::createSecureTicket - Validation passed, proceeding with ticket creation");

        String hashIpAddress = IpAddressService.extractAndHashIp(httpServletRequest);

        try {
            // 1. Create and configure ticket entity
            log.info("MsgXServiceImpl::createSecureTicket - Creating ticket entity");
            Ticket ticket = new Ticket();
            ticketBuilderService.configureTicketEntity(ticketCreationRequest, ticket, hashIpAddress);
            log.info("MsgXServiceImpl::createSecureTicket - Ticket entity configured");

            // 2. Encrypt message content
            log.info("MsgXServiceImpl::createSecureTicket - Encrypting message content");
            ticketBuilderService.encryptMessageContent(ticketCreationRequest, ticket);
            log.info("MsgXServiceImpl::createSecureTicket - Message content encrypted");

            // 3. Save ticket and passkeys
            log.info("MsgXServiceImpl::createSecureTicket - Saving ticket entity");
            Ticket savedTicket = ticketRepository.save(ticket);
            log.info("MsgXServiceImpl::createSecureTicket - Ticket saved with id: {}", savedTicket.getTicketId());

            log.info("MsgXServiceImpl::createSecureTicket - Saving passkeys");
            ticketBuilderService.savePasskeys(ticketCreationRequest.getPasskeys(), savedTicket);
            log.info("MsgXServiceImpl::createSecureTicket - Passkeys saved");

            // 4. Build and return response
            log.info("MsgXServiceImpl::createSecureTicket - Building creation response");
            TicketCreationResponse response = ticketBuilderService.buildCreationResponse(savedTicket, ticketCreationRequest.getPasskeys());
            log.info("MsgXServiceImpl::createSecureTicket - Ticket creation response built");

            return response;
        }
        catch (Exception ex) {
            log.error("Ticket creation failed: {}", ex.getMessage(), ex);
            throw new GlobalMsgXExceptions("Failed to create secure ticket: " + ex.getMessage());
        }
    }

    @Override
    public String permanentlyDeleteTicket(String ticketId) {
        log.info("MsgXServiceImpl::permanentlyDeleteTicket - Received request to permanently delete ticketId: {}", ticketId);
        return ticketRepository.findById(ticketId).map(ticket -> {
            ticketRepository.delete(ticket);
            log.info("MsgXServiceImpl::permanentlyDeleteTicket - Ticket with ID {} permanently deleted.", ticketId);
            return "Ticket ID: " + ticketId + " has been permanently deleted." +
                    "This action removed all associated encrypted content, metadata, access logs, replies, and passkeys from the system." +
                    "No trace of this ticket remains in our database or internal services — not even for audit, analytics, or recovery purposes. " +
                    "This is a complete and irreversible removal, done out of deep respect for your privacy and control." +
                    "When you choose to delete, it means total freedom — with zero digital residue. Your data. Your choice. Always.";

        }).orElseGet(() -> {
            log.warn("MsgXServiceImpl::permanentlyDeleteTicket - Ticket with ID {} not found.", ticketId);
            return "⚠️ Ticket not found. It may have already been deleted or never existed.";
        });
    }
}
