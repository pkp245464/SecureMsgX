package com.secure.MsgX.core.enums;

/**
 * TicketType defines message behavior for reply and visibility control.
 * SINGLE: One-time private message, no replies allowed.
 * SECURE_SINGLE: Highly sensitive message (1 view only), no replies.
 * THREAD: Private 1-to-1 conversation, replies allowed (requires parentTicketId for replies).
 * BROADCAST: One-to-many announcement, replies NOT allowed.
 * GROUP: Multi-person thread, replies allowed from participants (requires parentTicketId for context).
 */
public enum TicketType {
    SINGLE,
    SECURE_SINGLE,
    THREAD,
    BROADCAST,
    GROUP
}

