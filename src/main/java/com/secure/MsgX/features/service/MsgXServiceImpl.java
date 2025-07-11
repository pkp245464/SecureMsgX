package com.secure.MsgX.features.service;

import com.secure.MsgX.core.entity.Passkey;
import com.secure.MsgX.core.entity.ReadLog;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.accessDto.PasskeyEntry;
import com.secure.MsgX.features.dto.accessDto.ViewTicketRequest;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import com.secure.MsgX.features.repository.ReadLogRepository;
import com.secure.MsgX.features.repository.TicketRepository;
import com.secure.MsgX.features.utility.commonUtil.CryptoService;
import com.secure.MsgX.features.utility.commonUtil.IpAddressService;
import com.secure.MsgX.features.utility.ticketCreateUtil.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgXServiceImpl implements MsgXService{

    private final CryptoService cryptoService;
    private final ReadLogRepository readLogRepository;
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
        validateTicketType(ticket);
        log.info("MsgXServiceImpl::viewTicket - Ticket type is valid");

        // 3. Validate ticket status
        log.info("MsgXServiceImpl::viewTicket - Validating ticket status");
        validateTicketStatus(ticket);
        log.info("MsgXServiceImpl::viewTicket - Ticket status is valid");

        // 4. Validate access window
        log.info("MsgXServiceImpl::viewTicket - Validating access window (openFrom, openUntil, expiresAt)");
        validateAccessWindow(ticket);
        log.info("MsgXServiceImpl::viewTicket - Access window is valid");

        // 5. Validate view limits
        log.info("MsgXServiceImpl::viewTicket - Validating view count against maxViews");
        validateViewLimits(ticket);
        log.info("MsgXServiceImpl::viewTicket - View count is within allowed limit");

        // 6. Validate passkeys
        log.info("MsgXServiceImpl::viewTicket - Validating provided passkeys");
        validatePasskeys(ticket, request.getPasskeys());
        log.info("MsgXServiceImpl::viewTicket - Passkeys are valid");

        // 7. Process view
        log.info("MsgXServiceImpl::viewTicket - Processing ticket view");
        ViewTicketResponse response = processTicketView(ticket, request.getPasskeys(), clientIp);
        log.info("MsgXServiceImpl::viewTicket - Ticket viewed successfully. Returning decrypted content");

        return response;
    }

    private void validateTicketType(Ticket ticket) {
        TicketType ticketType = ticket.getTicketType();
        if(ticketType != TicketType.SINGLE && ticketType != TicketType.SECURE_SINGLE && ticketType != TicketType.BROADCAST) {
            throw new GlobalMsgXExceptions("Invalid ticket type for this operation. " + "This endpoint supports only SINGLE, SECURE_SINGLE and BROADCAST ticket type.");
        }
    }

    private void validateTicketStatus(Ticket ticket) {
        if (ticket.getTicketStatus() != TicketStatus.OPEN) {
            throw new GlobalMsgXExceptions( "This ticket is currently unavailable. Current status: " + ticket.getTicketStatus() +
                    ". It may have been viewed, expired, closed, or revoked by the sender. Please contact the sender if you believe this is an error.");
        }
    }
    private void validateAccessWindow(Ticket ticket) {
        Instant now = Instant.now();
        if (Objects.nonNull(ticket.getExpiresAt()) && now.isAfter(ticket.getExpiresAt())) {
            updateTicketStatus(ticket, TicketStatus.EXPIRED);
            throw new GlobalMsgXExceptions("This ticket is no longer accessible — it has passed its expiration window and is now marked as expired.");
        }
        if (Objects.nonNull(ticket.getOpenFrom()) && now.isBefore(ticket.getOpenFrom())) {
            throw new GlobalMsgXExceptions("This ticket is not yet available for viewing. Please check the scheduled open time and try again later.");
        }
        if (Objects.nonNull(ticket.getOpenUntil()) && now.isAfter(ticket.getOpenUntil())) {
            updateTicketStatus(ticket, TicketStatus.EXPIRED);
            throw new GlobalMsgXExceptions("The access window for this ticket has ended. This message is no longer available for viewing.");
        }
    }

    private void validateViewLimits(Ticket ticket) {
        if (Objects.nonNull(ticket.getMaxViews()) && ticket.getCountViews() >= ticket.getMaxViews()) {
            updateTicketStatus(ticket, TicketStatus.VIEW_LIMIT_REACHED);
            throw new GlobalMsgXExceptions("You have reached the maximum number of allowed views for this ticket. " +
                    "No further access is permitted. " +
                    "For additional access, please contact the creator of this ticket.");
        }
    }

    private void validatePasskeys(Ticket ticket, List<PasskeyEntry> passkeys) {
        List<PasskeyEntry> sortedEntries = passkeys.stream()
                .sorted(Comparator.comparingInt(PasskeyEntry::getOrder))
                .toList();

        List<Passkey> storedPasskeys = ticket.getPasskeys().stream()
                .sorted(Comparator.comparingInt(Passkey::getKeyOrder))
                .toList();

        if (sortedEntries.size() != storedPasskeys.size()) {
            throw new GlobalMsgXExceptions("Incorrect number of passkeys provided. Please ensure you submit the exact number of passkeys required to access this ticket.");
        }

        for (int i = 0; i < storedPasskeys.size(); i++) {
            Passkey stored = storedPasskeys.get(i);
            PasskeyEntry provided = sortedEntries.get(i);

            if (!cryptoService.verifyPasskey(provided.getValue(), stored.getPasskeyHash())) {
                throw new GlobalMsgXExceptions("Passkey order or passkey value is incorrect. " +
                        "Make sure you enter all passkeys in the correct order and with accurate values before retrying.");
            }
        }
    }

    private ViewTicketResponse processTicketView(Ticket ticket, List<PasskeyEntry> passkeys, String clientIp) {
        try {
            List<String> passkeyValues = passkeys.stream()
                    .sorted(Comparator.comparingInt(PasskeyEntry::getOrder))
                    .map(p -> p.getValue().trim())
                    .toList();

            String decryptedContent = cryptoService.decryptContent(
                    ticket.getEncryptedMessage(),
                    passkeyValues,
                    ticket.getSalt(),
                    ticket.getIv(),
                    ticket.getEncryptionAlgo()
            );

            // 2. Update view count
            ticket.setCountViews(ticket.getCountViews() + 1);

            // 3. Check if view limit reached
            if (ticket.getMaxViews() != null && ticket.getCountViews() >= ticket.getMaxViews()) {
                updateTicketStatus(ticket, TicketStatus.VIEW_LIMIT_REACHED);
            }

            // 4. Create read log
            createReadLog(ticket, clientIp);

            // 5. For SECURE_SINGLE tickets, close immediately after viewing
            if (ticket.getTicketType() == TicketType.SECURE_SINGLE) {
                updateTicketStatus(ticket, TicketStatus.CLOSED);
            }

            // 6. Build response
            return buildViewResponse(ticket, decryptedContent);
        }
        catch (Exception ex) {
            throw new GlobalMsgXExceptions("Unable to decrypt the ticket content. This may be due to incorrect passkeys, " +
                            "corrupted data, or an internal processing error. Please double-check your input " +
                            "and try again. If the issue persists, contact the ticket creator for further assistance.");
        }
    }

    private void updateTicketStatus(Ticket ticket, TicketStatus status) {
        ticket.setTicketStatus(status);
        ticketRepository.save(ticket);
        log.info("MsgXServiceImpl::updateTicketStatus - Updated ticket {} status to {}", ticket.getTicketId(), status);
    }

    private void createReadLog(Ticket ticket, String clientIp) {
        try {
            ReadLog readLog = new ReadLog();
            readLog.setTicket(ticket);

            String hashedIp = IpAddressService.hashIpAddress(clientIp);
            String entropy = UniqueIdGenerators.UlidGenerator.generateUlid();
            String rawValue = ticket.getSalt() + ":" + entropy + ":" + hashedIp;
            String fullyMixed = IpAddressService.shuffleAndShiftHash(rawValue, entropy);
            readLog.setReadByIpAddress(fullyMixed);

            readLogRepository.save(readLog);
            log.info("MsgXServiceImpl::CreateReadLog - Created read log for ticket {}", ticket.getTicketId());
        }
        catch (Exception e) {
            log.error("MsgXServiceImpl::CreateReadLog - Failed to create read log: {}", e.getMessage());
        }
    }

    private String buildIpHashSalt(String userSalt, String hashIpAddress) {
        String uuidEntropy = UniqueIdGenerators.UlidGenerator.generateUlid();
        return userSalt + ":" + uuidEntropy + ":" + hashIpAddress;
    }


    private ViewTicketResponse buildViewResponse(Ticket ticket, String decryptedContent) {
        ViewTicketResponse response = new ViewTicketResponse();
        response.setTicketNumber(ticket.getTicketNumber());
        response.setDecryptedContent(decryptedContent);
        response.setOpenFrom(ticket.getOpenFrom());
        response.setOpenUntil(ticket.getOpenUntil());
        response.setMaxViews(ticket.getMaxViews());
        response.setRemainingViews(ticket.getMaxViews() != null ? ticket.getMaxViews() - ticket.getCountViews() : null);
        response.setTicketStatus(ticket.getTicketStatus().name());
        response.setReadAt(Instant.now());

        if (ticket.getTicketType() == TicketType.SECURE_SINGLE) {
            response.setSecurityMessage("This was a SECURE_SINGLE ticket. The message content has now been permanently destroyed after viewing, in accordance with the one-time access policy. " +
                            "You will not be able to access this message again. SECURE_SINGLE tickets are designed for single-use only, and this behavior is enforced by default to ensure maximum confidentiality.");
        }
        return response;
    }
}
