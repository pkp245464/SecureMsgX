package com.secure.MsgX.features.utility;

import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.TicketCreationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class TicketCreationRequestValidator {

    public void validateRequest(TicketCreationRequest ticketCreationRequest) {
        log.info("TicketCreationRequestValidator::validateRequest - Starting validation of ticket request");

        if (Objects.isNull(ticketCreationRequest)) {
            log.error("TicketCreationRequestValidator::validateRequest failed - ticketCreationRequest is null");
            throw new GlobalMsgXExceptions("TicketCreationRequestValidator::validateRequest failed - ticketCreationRequest is null");
        }

        validateMessageContent(ticketCreationRequest);
        validateEncryptionAlgo(ticketCreationRequest);
        validatePasskeys(ticketCreationRequest);
        validateAccessTiming(ticketCreationRequest);
        validateMaxViews(ticketCreationRequest);
        validateReplyTicketFields(ticketCreationRequest);

        log.info("TicketCreationRequestValidator::validateRequest - Ticket request validation successful");
    }

    private void validateMessageContent(TicketCreationRequest request) {
        if (Objects.isNull(request.getMessageContent()) || request.getMessageContent().trim().isEmpty()) {
            log.error("TicketCreationRequestValidator::validateMessageContent failed - messageContent is null or empty");
            throw new GlobalMsgXExceptions("TicketCreationRequestValidator::validateMessageContent failed - messageContent is null or empty");
        }
    }

    private void validateEncryptionAlgo(TicketCreationRequest request) {
        if (Objects.isNull(request.getEncryptionAlgo())) {
            log.error("TicketCreationRequestValidator::validateEncryptionAlgo failed - encryptionAlgo is null");
            throw new GlobalMsgXExceptions("TicketCreationRequestValidator::validateEncryptionAlgo failed - encryptionAlgo is null");
        }
    }

    private void validatePasskeys(TicketCreationRequest request) {
        if (Objects.isNull(request.getPasskeys()) || request.getPasskeys().isEmpty()) {
            log.error("TicketCreationRequestValidator::validatePasskeys failed - passkeys list is null or empty");
            throw new GlobalMsgXExceptions("TicketCreationRequestValidator::validatePasskeys failed - At least one passkey is required");
        }

        int size = request.getPasskeys().size();
        if (size > 10) {
            log.error("TicketCreationRequestValidator::validatePasskeys failed - passkeys list contains more than 10 items");
            throw new GlobalMsgXExceptions("TicketCreationRequestValidator::validatePasskeys failed - No more than 10 passkeys are allowed");
        }
    }

    private void validateAccessTiming(TicketCreationRequest request) {
        boolean hasExpiresAt = request.getExpiresAt() != null;
        boolean hasOpenWindow = request.getOpenFrom() != null && request.getOpenUntil() != null;

        if (hasExpiresAt && hasOpenWindow) {
            log.error("TicketCreationRequestValidator::validateAccessTiming failed - Both expiresAt and openFrom/openUntil are provided");
            throw new GlobalMsgXExceptions("TicketCreationRequestValidator::validateAccessTiming failed - Provide either expiresAt OR openFrom/openUntil, not both");
        }

        if (hasOpenWindow) {
            if (request.getOpenFrom().isAfter(request.getOpenUntil())) {
                log.error("TicketCreationRequestValidator::validateAccessTiming failed - openFrom is after openUntil");
                throw new GlobalMsgXExceptions("openFrom must be before openUntil");
            }
            request.setExpiresAt(null);
        }
        else if (hasExpiresAt) {
            request.setOpenFrom(java.time.Instant.now());
            request.setOpenUntil(request.getExpiresAt());
            log.info("TicketCreationRequestValidator::validateAccessTiming - Only expiresAt provided; auto-setting openFrom=now and openUntil=expiresAt");
        }
        else {
            log.error("TicketCreationRequestValidator::validateAccessTiming failed - No expiration info provided");
            throw new GlobalMsgXExceptions("Ticket must include either expiresAt or both openFrom & openUntil");
        }
    }

    private void validateMaxViews(TicketCreationRequest request) {
        if (Objects.isNull(request.getMaxViews()) || request.getMaxViews() < 1) {
            log.info("TicketCreationRequestValidator::validateMaxViews - maxViews is null or less than 1, setting default value to 1");
            request.setMaxViews(1);
        }
    }

    private void validateReplyTicketFields(TicketCreationRequest request) {
        if (Objects.isNull(request.getTicketType())) {
            log.error("TicketCreationRequestValidator::validateReplyTicketFields failed - ticketType is null");
            throw new GlobalMsgXExceptions(
                    "ticketType is required. Please specify one of the following valid ticket types:\n" +
                            "SINGLE - One-time private message, no replies allowed, limited to 5 views.\n" +
                            "SECURE_SINGLE - Highly sensitive message (one view only), no replies.\n" +
                            "THREAD - Private 1-to-1 conversation, unlimited replies allowed.\n" +
                            "BROADCAST - One-to-many announcement, replies NOT allowed.\n" +
                            "GROUP - Multi-person thread, unlimited replies allowed from participants."
            );
        }

        switch (request.getTicketType()) {
            case SECURE_SINGLE:
                request.setAllowReplies(false);
                request.setMaxViews(1);
                log.info("TicketCreationRequestValidator::validateReplyTicketFields - Configured SECURE_SINGLE ticket: maxViews=1, allowReplies=false");
                break;

            case SINGLE:
                request.setAllowReplies(false);
                request.setMaxViews(5);
                log.info("TicketCreationRequestValidator::validateReplyTicketFields - Configured SINGLE ticket: maxViews=5, allowReplies=false");
                break;

            case THREAD:
                request.setAllowReplies(true);
                log.info("TicketCreationRequestValidator::validateReplyTicketFields - Configured THREAD ticket: allowReplies=true");
                break;

            case BROADCAST:
                if (Objects.isNull(request.getMaxViews())) {
                    log.error("TicketCreationRequestValidator::validateReplyTicketFields failed - maxViews is required for BROADCAST tickets");
                    throw new GlobalMsgXExceptions("maxViews is required for BROADCAST tickets");
                }
                request.setAllowReplies(false);
                log.info("TicketCreationRequestValidator::validateReplyTicketFields - Configured BROADCAST ticket: allowReplies=false");
                break;

            case GROUP:
                if (Objects.isNull(request.getMaxViews())) {
                    log.error("TicketCreationRequestValidator::validateReplyTicketFields failed - maxViews is required for GROUP tickets");
                    throw new GlobalMsgXExceptions("maxViews is required for GROUP tickets");
                }
                request.setAllowReplies(true);
                log.info("TicketCreationRequestValidator::validateReplyTicketFields - Configured GROUP ticket: allowReplies=true");
                break;

            default:
                log.error("TicketCreationRequestValidator::validateReplyTicketFields failed - Unknown ticketType: {}", request.getTicketType());
                throw new GlobalMsgXExceptions(
                        "Unknown ticketType: " + request.getTicketType() + ". Please select a valid ticket type:\n" +
                                "SINGLE - One-time private message, no replies allowed, limited to 5 views.\n" +
                                "SECURE_SINGLE - Highly sensitive message (one view only), no replies.\n" +
                                "THREAD - Private 1-to-1 conversation, unlimited replies allowed.\n" +
                                "BROADCAST - One-to-many announcement, replies NOT allowed.\n" +
                                "GROUP - Multi-person thread, unlimited replies allowed from participants."
                );

        }
    }
}
