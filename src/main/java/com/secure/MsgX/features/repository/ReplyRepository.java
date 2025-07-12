package com.secure.MsgX.features.repository;

import com.secure.MsgX.core.entity.Reply;
import com.secure.MsgX.core.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, String> {
    @EntityGraph(attributePaths = {"childReplies"})
    List<Reply> findByTicketAndParentReplyIsNullOrderByCreatedAtAsc(Ticket ticket);
}
