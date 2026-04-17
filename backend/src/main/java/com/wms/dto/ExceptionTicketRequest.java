package com.wms.dto;

import lombok.Data;

@Data
public class ExceptionTicketRequest {

    private Long warehouseId;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String ticketTitle;
    private String ticketContent;
    private String severity;
    private String assigneeName;
    private String reporterName;
}
