package com.secure.MsgX.features.service;

import com.secure.MsgX.core.entity.Passkey;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketStatus;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.PasskeyDto;
import com.secure.MsgX.features.dto.TicketCreationRequest;
import com.secure.MsgX.features.dto.TicketCreationResponse;
import com.secure.MsgX.features.repository.PasskeyRepository;
import com.secure.MsgX.features.repository.TicketRepository;
import com.secure.MsgX.features.utility.CryptoService;
import com.secure.MsgX.features.utility.IpAddressService;
import com.secure.MsgX.features.utility.TicketCreationRequestValidator;
import com.secure.MsgX.features.utility.UniqueIdGenerators;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgXServiceImpl implements MsgXService{

    private final CryptoService cryptoService;
    private final PasskeyRepository passkeyRepository;
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
            configureTicketEntity(ticketCreationRequest, ticket, hashIpAddress);
            log.info("MsgXServiceImpl::createSecureTicket - Ticket entity configured");

            // 2. Encrypt message content
            log.info("MsgXServiceImpl::createSecureTicket - Encrypting message content");
            encryptMessageContent(ticketCreationRequest, ticket);
            log.info("MsgXServiceImpl::createSecureTicket - Message content encrypted");

            // 3. Save ticket and passkeys
            log.info("MsgXServiceImpl::createSecureTicket - Saving ticket entity");
            Ticket savedTicket = ticketRepository.save(ticket);
            log.info("MsgXServiceImpl::createSecureTicket - Ticket saved with id: {}", savedTicket.getTicketId());

            log.info("MsgXServiceImpl::createSecureTicket - Saving passkeys");
            savePasskeys(ticketCreationRequest.getPasskeys(), savedTicket);
            log.info("MsgXServiceImpl::createSecureTicket - Passkeys saved");

            // 4. Build and return response
            log.info("MsgXServiceImpl::createSecureTicket - Building creation response");
            TicketCreationResponse response = buildCreationResponse(savedTicket, ticketCreationRequest.getPasskeys());
            log.info("MsgXServiceImpl::createSecureTicket - Ticket creation response built");

            return response;
        }
        catch (Exception ex) {
            log.error("Ticket creation failed: {}", ex.getMessage(), ex);
            throw new GlobalMsgXExceptions("Failed to create secure ticket: " + ex.getMessage());
        }
    }

    private void configureTicketEntity(TicketCreationRequest request, Ticket ticket, String hashIpAddress) {
        ticket.setTicketNumber(UniqueIdGenerators.UlidGenerator.generateUlid());
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

    private void encryptMessageContent(TicketCreationRequest request, Ticket ticket) {
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

    private void savePasskeys(List<String> passkeys, Ticket ticket) {
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

    private TicketCreationResponse buildCreationResponse(Ticket ticket, List<String> originalPasskeys) {
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
                    return dto;
                })
                .toList();

        response.setPasskey(passkeyDtos);
        return response;
    }
}
