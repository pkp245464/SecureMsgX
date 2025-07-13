package com.secure.MsgX.features.utility.accessUtil;

import com.secure.MsgX.core.entity.Passkey;
import com.secure.MsgX.core.entity.ReadLog;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.commonDto.PasskeyEntry;
import com.secure.MsgX.features.dto.accessDto.ViewTicketResponse;
import com.secure.MsgX.features.repository.ReadLogRepository;
import com.secure.MsgX.features.repository.TicketRepository;
import com.secure.MsgX.features.utility.commonUtil.CryptoService;
import com.secure.MsgX.features.utility.commonUtil.IpAddressService;
import com.secure.MsgX.features.utility.ticketCreateUtil.UniqueIdGenerators;
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
public class TicketViewBuilderService {

    private final CryptoService cryptoService;
    private final TicketRepository ticketRepository;
    private final ReadLogRepository readLogRepository;

    public void validateTicketType(Ticket ticket) {
        TicketType ticketType = ticket.getTicketType();
        if(ticketType != TicketType.SINGLE && ticketType != TicketType.SECURE_SINGLE && ticketType != TicketType.BROADCAST) {
            throw new GlobalMsgXExceptions("Invalid ticket type for this operation. " + "This endpoint supports only SINGLE, SECURE_SINGLE and BROADCAST ticket type.");
        }
    }

    public void validateTicketStatus(Ticket ticket) {
        if (ticket.getTicketStatus() != TicketStatus.OPEN) {
            throw new GlobalMsgXExceptions( "This ticket is currently unavailable. Current status: " + ticket.getTicketStatus() +
                    ". It may have been viewed, expired, closed, or revoked by the sender. Please contact the sender if you believe this is an error.");
        }
    }
    public void validateAccessWindow(Ticket ticket) {
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

    public void validateViewLimits(Ticket ticket) {
        if (Objects.nonNull(ticket.getMaxViews()) && ticket.getCountViews() >= ticket.getMaxViews()) {
            updateTicketStatus(ticket, TicketStatus.VIEW_LIMIT_REACHED);
            throw new GlobalMsgXExceptions("You have reached the maximum number of allowed views for this ticket. " +
                    "No further access is permitted. " +
                    "For additional access, please contact the creator of this ticket.");
        }
    }

    public void validatePasskeys(Ticket ticket, List<PasskeyEntry> passkeys) {
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

    public ViewTicketResponse processTicketView(Ticket ticket, List<PasskeyEntry> passkeys, String clientIp) {
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

    public void createReadLog(Ticket ticket, String clientIp) {
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
