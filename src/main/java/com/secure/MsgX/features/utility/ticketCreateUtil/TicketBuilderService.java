package com.secure.MsgX.features.utility.ticketCreateUtil;

import com.secure.MsgX.core.entity.Passkey;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.ticketCreateDto.PasskeyDto;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationRequest;
import com.secure.MsgX.features.dto.ticketCreateDto.TicketCreationResponse;
import com.secure.MsgX.features.repository.PasskeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketBuilderService {

    private final PasskeyRepository passkeyRepository;
    private final CryptoService cryptoService;

    public void configureTicketEntity(TicketCreationRequest request, Ticket ticket, String hashIpAddress) {
        String ticketPrefix = getTicketTypePrefix(request.getTicketType());
        String ticketIdentifier = UniqueIdGenerators.UlidGenerator.generateUlid();
        String ticketNumber = ticketPrefix + "-" + ticketIdentifier;
        ticket.setTicketNumber(ticketNumber);
        ticket.setTicketType(request.getTicketType());
        ticket.setAllowReplies(request.isAllowReplies());
        ticket.setMaxViews(request.getMaxViews());
        ticket.setExpiresAt(request.getExpiresAt());
        ticket.setOpenFrom(request.getOpenFrom());
        ticket.setOpenUntil(request.getOpenUntil());
        ticket.setEncryptionAlgo(request.getEncryptionAlgo());

        ticket.setTicketStatus(TicketStatus.OPEN);

        ticket.setCountViews(0);

        String salt = request.getSalt() != null ? request.getSalt() : cryptoService.generateSalt();
        ticket.setSalt(salt);

        String ipHashSalt  = buildIpHashSalt(salt, hashIpAddress);
        ticket.setCreatorIpAddress(ipHashSalt );
    }

    private String buildIpHashSalt(String userSalt, String hashIpAddress) {
        String uuidEntropy = UniqueIdGenerators.UlidGenerator.generateUlid();
        return userSalt + ":" + uuidEntropy + ":" + hashIpAddress;
    }

    public void encryptMessageContent(TicketCreationRequest request, Ticket ticket) {
        try {
            byte[] encryptedContent = cryptoService.encryptContent(
                    request.getMessageContent(),
                    request.getPasskeys(),
                    ticket.getSalt(),
                    request.getEncryptionAlgo()
            );
            ticket.setEncryptedMessage(encryptedContent);
            ticket.setIv(cryptoService.getLastGeneratedIV());
        }
        catch (GlobalMsgXExceptions ex) {
            throw new GlobalMsgXExceptions("Encryption failed during ticket creation", ex);
        }
    }

    public void savePasskeys(List<String> passkeys, Ticket ticket) {
        List<Passkey> passkeyEntities = IntStream.range(0, passkeys.size())
                .mapToObj(i -> {
                    Passkey passkey = new Passkey();
                    passkey.setPasskeyHash(cryptoService.hashPasskey(passkeys.get(i)));
                    passkey.setKeyOrder(i + 1);
                    passkey.setTicket(ticket);
                    return passkey;
                })
                .toList();

        passkeyRepository.saveAll(passkeyEntities);
        log.info("Saved {} passkeys for ticket {}", passkeys.size(), ticket.getTicketId());
    }

    public TicketCreationResponse buildCreationResponse(Ticket ticket, List<String> originalPasskeys) {
        TicketCreationResponse response = new TicketCreationResponse();
        response.setTicketId(ticket.getTicketId());
        response.setTicketNumber(ticket.getTicketNumber());
        response.setExpiresAt(ticket.getExpiresAt());
        response.setOpenFrom(ticket.getOpenFrom());
        response.setOpenUntil(ticket.getOpenUntil());
        response.setEncryptionAlgo(ticket.getEncryptionAlgo());
        response.setTicketStatus(ticket.getTicketStatus());
        response.setTicketType(ticket.getTicketType());
        response.setAllowReplies(ticket.isAllowReplies());
        response.setCountViews(ticket.getCountViews());
        response.setSalt(ticket.getSalt());

        List<PasskeyDto> passkeyDtos = IntStream.range(0, originalPasskeys.size())
                .mapToObj(i -> {
                    PasskeyDto dto = new PasskeyDto();
                    dto.setKeyOrder(i + 1);
                    dto.setPasskeyHash("[PROTECTED]");
                    dto.setPasskey(originalPasskeys.get(i));
                    return dto;
                })
                .toList();

        response.setPasskey(passkeyDtos);
        return response;
    }

    private String getTicketTypePrefix(TicketType type) {
        return switch (type) {
            case SINGLE -> "SGL";
            case SECURE_SINGLE -> "SSL";
            case THREAD -> "THD";
            case BROADCAST -> "BRC";
            case GROUP -> "GRP";
        };
    }
}
