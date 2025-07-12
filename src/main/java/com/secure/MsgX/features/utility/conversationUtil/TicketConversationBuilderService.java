package com.secure.MsgX.features.utility.conversationUtil;

import com.secure.MsgX.core.entity.Reply;
import com.secure.MsgX.core.entity.Ticket;
import com.secure.MsgX.core.enums.TicketType;
import com.secure.MsgX.core.exceptions.GlobalMsgXExceptions;
import com.secure.MsgX.features.dto.accessConversationDto.ConversationNode;
import com.secure.MsgX.features.dto.accessConversationDto.ViewConversationResponse;
import com.secure.MsgX.features.utility.commonUtil.CryptoService;
import com.secure.MsgX.features.utility.commonUtil.IpAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketConversationBuilderService {

    private final CryptoService cryptoService;

    public Reply buildReplyEntity(String encryptedReply, String iv, Ticket ticket, Reply parentReply, String clientIp) {
        Reply reply = new Reply();
        reply.setEncryptedContent(encryptedReply);
        reply.setIv(iv);
        reply.setTicket(ticket);
        reply.setParentReply(parentReply);
        reply.setReplyIpAddress(IpAddressService.hashIpAddress(clientIp));
        return reply;
    }

    public void validateConversationTicket(Ticket ticket) {
        if (ticket.getTicketType() != TicketType.THREAD &&
                ticket.getTicketType() != TicketType.GROUP) {
            throw new GlobalMsgXExceptions("This operation is only valid for THREAD and GROUP tickets");
        }
    }

    public List<ConversationNode> buildConversationTree(List<Reply> replies, List<String> passkeyValues, Ticket ticket) {
        return replies.stream().map(reply -> {
            ConversationNode node = new ConversationNode();
            node.setReplyId(reply.getReplyId());
            node.setCreatedAt(reply.getCreatedAt());

            // Decrypt reply content
            String decryptedContent = cryptoService.decryptContent(
                    reply.getEncryptedContent(),
                    passkeyValues,
                    ticket.getSalt(),
                    reply.getIv(),
                    ticket.getEncryptionAlgo()
            );
            node.setDecryptedContent(decryptedContent);

            // Recursively build child replies
            if (!reply.getChildReplies().isEmpty()) {
                List<Reply> sortedChildren = reply.getChildReplies().stream()
                        .sorted(Comparator.comparing(Reply::getCreatedAt))
                        .collect(Collectors.toList());

                node.setReplies(buildConversationTree(sortedChildren, passkeyValues, ticket));
            }
            return node;
        }).collect(Collectors.toList());
    }

    public ViewConversationResponse buildResponse(Ticket ticket, String decryptedContent, List<ConversationNode> conversation) {
        ViewConversationResponse response = new ViewConversationResponse();
        response.setTicketNumber(ticket.getTicketNumber());
        response.setDecryptedContent(decryptedContent);
        response.setOpenFrom(ticket.getOpenFrom());
        response.setOpenUntil(ticket.getOpenUntil());
        response.setMaxViews(ticket.getMaxViews());
        response.setRemainingViews(ticket.getMaxViews() != null ? ticket.getMaxViews() - ticket.getCountViews() : null);
        response.setTicketStatus(ticket.getTicketStatus().name());
        response.setReadAt(Instant.now());
        response.setConversation(conversation);
        return response;
    }
}
