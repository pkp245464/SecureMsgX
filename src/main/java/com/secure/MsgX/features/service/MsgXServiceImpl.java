package com.secure.MsgX.features.service;


import com.secure.MsgX.core.entity.Reply;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.accessConversationDto.*;
import com.secure.MsgX.features.dto.commonDto.PasskeyEntry;
import com.secure.MsgX.features.dto.commonDto.UnifiedViewRequest;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import com.secure.MsgX.features.repository.ReplyRepository;
import com.secure.MsgX.features.repository.TicketRepository;
import com.secure.MsgX.features.utility.accessUtil.TicketViewBuilderService;
import com.secure.MsgX.features.utility.commonUtil.CryptoService;
import com.secure.MsgX.features.utility.commonUtil.IpAddressService;
import com.secure.MsgX.features.utility.conversationUtil.TicketConversationBuilderService;
import com.secure.MsgX.features.utility.ticketCreateUtil.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MsgXServiceImpl implements MsgXService{

    private final CryptoService cryptoService;

    private final TicketRepository ticketRepository;
    private final ReplyRepository replyRepository;

    private final TicketCreationRequestValidator ticketCreationRequestValidator;

    private final TicketBuilderService ticketBuilderService;
    private final TicketViewBuilderService ticketViewBuilderService;
    private final TicketConversationBuilderService ticketConversationBuilderService;

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
    public Object viewUnifiedTicket(UnifiedViewRequest request, String clientIp) {
        log.info("MsgXServiceImpl::viewUnifiedTicket - Request for ticketNumber: {}", request.getTicketNumber());

        // 1. Fetch ticket
        Ticket ticket = ticketRepository.findByTicketNumber(request.getTicketNumber())
                .orElseThrow(() -> {
                    log.warn("Ticket not found: {}", request.getTicketNumber());
                    return new GlobalMsgXExceptions("The requested ticket does not exist or has been permanently removed. Please verify the ticket number and try again.");
                });

        switch (ticket.getTicketType()) {
            case SINGLE, SECURE_SINGLE, BROADCAST -> {
                return viewTicket(request, clientIp, ticket);
            }
            case THREAD, GROUP -> {
                return viewConversation(request, clientIp, ticket);
            }
            default -> throw new GlobalMsgXExceptions(
                    "Oops — the ticket type you're using isn't compatible with this action. " +
                            "We currently support the following ticket types: SINGLE, SECURE_SINGLE, BROADCAST, THREAD, and GROUP. " +
                            "Please check your ticket and try again. If you believe this is an error, feel free to reach out to support for help."
            );
        }
    }

    public ViewTicketResponse viewTicket(UnifiedViewRequest request, String clientIp, Ticket ticket) {
        log.info("MsgXServiceImpl::viewTicket - Received request to view ticket: {}", request.getTicketNumber());

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


    public ViewConversationResponse viewConversation(UnifiedViewRequest request, String clientIp, Ticket ticket) {
        log.info("MsgXServiceImpl::viewConversation - Received request to view conversation for ticket: {}", request.getTicketNumber());

        // 2. Validate ticket type
        log.info("MsgXServiceImpl::viewConversation - Validating ticket type");
        ticketConversationBuilderService.validateConversationTicket(ticket);
        log.info("MsgXServiceImpl::viewConversation - Ticket type is valid for conversation");

        // 3. Validate ticket status
        log.info("MsgXServiceImpl::viewConversation - Validating ticket status");
        ticketViewBuilderService.validateTicketStatus(ticket);
        log.info("MsgXServiceImpl::viewConversation - Ticket status is valid");

        // 4. Validate access window
        log.info("MsgXServiceImpl::viewConversation - Validating access window");
        ticketViewBuilderService.validateAccessWindow(ticket);
        log.info("MsgXServiceImpl::viewConversation - Access window is valid");

        // 5. Validate view limits
        log.info("MsgXServiceImpl::viewConversation - Validating view limits");
        ticketViewBuilderService.validateViewLimits(ticket);
        log.info("MsgXServiceImpl::viewConversation - View count within allowed limits");

        // 6. Validate passkeys
        log.info("MsgXServiceImpl::viewConversation - Validating passkeys");
        ticketViewBuilderService.validatePasskeys(ticket, request.getPasskeys());
        log.info("MsgXServiceImpl::viewConversation - Passkeys are valid");

        // 7. Extract and sort passkey values
        log.info("MsgXServiceImpl::viewConversation - Extracting and sorting passkeys");
        List<String> passkeyValues = request.getPasskeys().stream()
                .sorted(Comparator.comparingInt(PasskeyEntry::getOrder))
                .map(p -> p.getValue().trim())
                .collect(Collectors.toList());

        // 8. Update view count and log access
        log.info("MsgXServiceImpl::viewConversation - Updating view count and logging read event");
        ticket.setCountViews(ticket.getCountViews() + 1);
        ticketViewBuilderService.createReadLog(ticket, clientIp);

        if (Objects.nonNull(ticket.getMaxViews())  && ticket.getCountViews() >= ticket.getMaxViews()) {
            log.info("MsgXServiceImpl::viewConversation - View limit reached for ticket. Updating status.");
            ticket.setTicketStatus(TicketStatus.VIEW_LIMIT_REACHED);
        }
        ticketRepository.save(ticket);
        log.info("MsgXServiceImpl::viewConversation - View count updated and ticket saved");

        // 9. Decrypt ticket content
        log.info("MsgXServiceImpl::viewConversation - Decrypting main message content");
        String decryptedContent = cryptoService.decryptContent(
                ticket.getEncryptedMessage(),
                passkeyValues,
                ticket.getSalt(),
                ticket.getIv(),
                ticket.getEncryptionAlgo()
        );
        log.info("MsgXServiceImpl::viewConversation - Message content decrypted successfully");

        // 10. Build a conversation tree
        log.info("MsgXServiceImpl::viewConversation - Building conversation tree from replies");
        List<Reply> topLevelReplies = replyRepository.findByTicketAndParentReplyIsNullOrderByCreatedAtAsc(ticket);
        List<ConversationNode> conversationTree = ticketConversationBuilderService.buildConversationTree(topLevelReplies, passkeyValues, ticket);
        log.info("MsgXServiceImpl::viewConversation - Conversation tree built with {} top-level replies", conversationTree.size());

        // 11. Build and return response
        log.info("MsgXServiceImpl::viewConversation - Building and returning response");
        return ticketConversationBuilderService.buildResponse(ticket, decryptedContent, conversationTree);
    }

    @Override
    public PostReplyResponse postReply(PostReplyRequest request, String clientIp) {
        log.info("MsgXServiceImpl::postReply - Received request to post reply to ticket: {}", request.getTicketNumber());

        // 1. Fetch the ticket
        log.info("MsgXServiceImpl::postReply - Fetching ticket from repository");
        Ticket ticket = ticketRepository.findByTicketNumber(request.getTicketNumber())
                .orElseThrow(() -> {
                    log.warn("MsgXServiceImpl::postReply - Ticket not found: {}", request.getTicketNumber());
                            return new GlobalMsgXExceptions("The requested ticket does not exist or has been permanently removed. Please verify the ticket number and try again.");
                });
        log.info("MsgXServiceImpl::postReply - Ticket found with ID: {}", ticket.getTicketId());

        // 2. Validate ticket type
        log.info("MsgXServiceImpl::postReply - Validating ticket type");
        ticketConversationBuilderService.validateConversationTicket(ticket);

        // 3. Validate ticket status
        log.info("MsgXServiceImpl::postReply - Validating ticket status");
        ticketViewBuilderService.validateTicketStatus(ticket);

        // 4. Validate access window
        log.info("MsgXServiceImpl::postReply - Validating access window");
        ticketViewBuilderService.validateAccessWindow(ticket);

        // 5. Validate passkeys
        log.info("MsgXServiceImpl::postReply - Validating provided passkeys");
        ticketViewBuilderService.validatePasskeys(ticket, request.getPasskeys());
        log.info("MsgXServiceImpl::postReply - Passkeys are valid");

        // 6. Process and sort passkeys
        log.info("MsgXServiceImpl::postReply - Sorting and processing passkeys");
        List<String> passkeyValues = request.getPasskeys().stream()
                .sorted(Comparator.comparingInt(PasskeyEntry::getOrder))
                .map(p -> p.getValue().trim())
                .collect(Collectors.toList());

        // 7. Fetch parent reply if applicable
        Reply parentReply = null;
        if (request.getParentReplyId() != null) {
            log.info("MsgXServiceImpl::postReply - Fetching parent reply with ID: {}", request.getParentReplyId());
            parentReply = replyRepository.findById(request.getParentReplyId())
                    .orElseThrow(() ->{
                        log.warn("MsgXServiceImpl::postReply - Parent reply not found: {}", request.getParentReplyId());
                        return new GlobalMsgXExceptions("Parent reply not found");
                    });
            if (!parentReply.getTicket().getTicketId().equals(ticket.getTicketId())) {
                log.error("MsgXServiceImpl::postReply - Parent reply belongs to a different ticket");
                throw new GlobalMsgXExceptions("Parent reply belongs to different ticket");
            }
            log.info("MsgXServiceImpl::postReply - Parent reply validated");
        }

        // 8. Encrypt reply content
        log.info("MsgXServiceImpl::postReply - Encrypting reply content");
        String encryptedReply = cryptoService.encryptContent(
                request.getContent(),
                passkeyValues,
                ticket.getSalt(),
                ticket.getEncryptionAlgo()
        );
        String iv = cryptoService.getLastGeneratedIVAsBase64();
        log.info("MsgXServiceImpl::postReply - Reply content encrypted");

        log.info("MsgXServiceImpl::postReply - Creating reply entity");
        Reply reply = ticketConversationBuilderService.buildReplyEntity(encryptedReply, iv, ticket, parentReply, clientIp);

        // 9. Save reply
        log.info("MsgXServiceImpl::postReply - Saving reply to repository");
        Reply savedReply = replyRepository.save(reply);
        log.info("MsgXServiceImpl::postReply - Reply saved with ID: {}", savedReply.getReplyId());

        // 10. Return response
        log.info("MsgXServiceImpl::postReply - Returning success response for posted reply");
        return new PostReplyResponse(savedReply.getReplyId(), "Reply posted successfully");
    }
}
