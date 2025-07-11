package com.secure.MsgX.features.repository;

import com.secure.MsgX.core.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,String> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
}
