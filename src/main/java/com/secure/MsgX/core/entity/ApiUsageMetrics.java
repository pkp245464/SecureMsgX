package com.secure.MsgX.core.entity;

import com.secure.MsgX.core.enums.TicketType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "api_usage_metrics")
public class ApiUsageMetrics {

    @Id
    @Column(name = "api_usage_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String apiUsageId;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type")
    private TicketType ticketType;

    @Column(name = "hit_count")
    private Long hitCount = 0L;
}
