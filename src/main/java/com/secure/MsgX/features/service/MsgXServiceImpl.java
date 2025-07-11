package com.secure.MsgX.features.service;


import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.accessDto.ViewTicketRequest;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import com.secure.MsgX.features.repository.TicketRepository;
import com.secure.MsgX.features.utility.accessUtil.TicketViewBuilderService;
import com.secure.MsgX.features.utility.commonUtil.IpAddressService;
import com.secure.MsgX.features.utility.ticketCreateUtil.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



@Slf4j
@Service
@RequiredArgsConstructor
public class MsgXServiceImpl implements MsgXService{

    private final TicketViewBuilderService ticketViewBuilderService;
    private final TicketRepository ticketRepository;
    private final TicketBuilderService ticketBuilderService;
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
            return "Ticket not found. It may have already been deleted or never existed.";
        });
    }

    @Override
    public ViewTicketResponse viewTicket(ViewTicketRequest request, String clientIp) {
        log.info("MsgXServiceImpl::viewTicket - Received request to view ticket: {}", request.getTicketNumber());

        // 1. Fetch ticket
        Ticket ticket = ticketRepository.findByTicketNumber(request.getTicketNumber())
                .orElseThrow(() -> {
                    log.warn("MsgXServiceImpl::viewTicket - Ticket not found for ticket number: {}", request.getTicketNumber());
                    return new GlobalMsgXExceptions("The requested ticket does not exist or has been permanently removed. Please verify the ticket number and try again.");
                });

        log.info("MsgXServiceImpl::viewTicket - Found ticket ID: {} with type: {}", ticket.getTicketId(), ticket.getTicketType());

        // 2. Validate ticket type
        log.info("MsgXServiceImpl::viewTicket - Validating ticket type");
        ticketViewBuilderService.validateTicketType(ticket);
        log.info("MsgXServiceImpl::viewTicket - Ticket type is valid");

        // 3. Validate ticket status
        log.info("MsgXServiceImpl::viewTicket - Validating ticket status");
        ticketViewBuilderService.validateTicketStatus(ticket);
        log.info("MsgXServiceImpl::viewTicket - Ticket status is valid");

        // 4. Validate access window
        log.info("MsgXServiceImpl::viewTicket - Validating access window (openFrom, openUntil, expiresAt)");
        ticketViewBuilderService.validateAccessWindow(ticket);
        log.info("MsgXServiceImpl::viewTicket - Access window is valid");

        // 5. Validate view limits
        log.info("MsgXServiceImpl::viewTicket - Validating view count against maxViews");
        ticketViewBuilderService.validateViewLimits(ticket);
        log.info("MsgXServiceImpl::viewTicket - View count is within allowed limit");

        // 6. Validate passkeys
        log.info("MsgXServiceImpl::viewTicket - Validating provided passkeys");
        ticketViewBuilderService.validatePasskeys(ticket, request.getPasskeys());
        log.info("MsgXServiceImpl::viewTicket - Passkeys are valid");

        // 7. Process view
        log.info("MsgXServiceImpl::viewTicket - Processing ticket view");
        ViewTicketResponse response = ticketViewBuilderService.processTicketView(ticket, request.getPasskeys(), clientIp);
        log.info("MsgXServiceImpl::viewTicket - Ticket viewed successfully. Returning decrypted content");
        return response;
    }
}
