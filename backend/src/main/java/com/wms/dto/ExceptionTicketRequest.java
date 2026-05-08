package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExceptionTicketRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long warehouseId;
    private String bizType;
    private Long bizId;
    private String bizNo;
    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String ticketTitle;
    private String ticketContent;
    private String severity;
    private String assigneeName;
    private String reporterName;
}
