package com.secure.MsgX.core.enums;

/**
 * TicketType defines message behavior for reply and visibility control.

 * Replies AREN'T Allowed:
 *  - SINGLE: One-time private message, up to 5 views allowed.
 *  - SECURE_SINGLE: Highly sensitive message, only 1 view allowed.
 *  - BROADCAST: One-to-many announcement, user can set custom view limit (range: 1 to 1,000,000,000).

 * Replies Allowed:
 *  - THREAD: Private 1-to-1 conversation, replies allowed (requires parentTicketId for replies).
 *  - GROUP: Multi-person thread, replies allowed from participants (requires parentTicketId for context).
 */


public enum TicketType {
    SINGLE,
    SECURE_SINGLE,
    THREAD,
    BROADCAST,
    GROUP
}

