package com.secure.MsgX.core.enums;

/**
 * TicketStatus represents the current lifecycle state of a ticket.
 * OPEN: Ticket is active and accessible.
 * EXPIRED: A ticket validity period has ended, no longer accessible.
 * CLOSED: Ticket was manually closed by the creator or system.
 * VIEW_LIMIT_REACHED: Ticket has been viewed the maximum allowed times.
 * REVOKED: Ticket access has been revoked, temporarily or permanently disabling use.
 */
public enum TicketStatus {
    OPEN,
    EXPIRED,
    CLOSED,
    VIEW_LIMIT_REACHED,
    REVOKED
}
///  Note: For VIEW_LIMIT_REACHED and REVOKED, the creator has the authority to increase limits or restore access.
