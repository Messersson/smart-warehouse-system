package com.wms.dto;

import lombok.Data;

@Data
public class OutboundScanRequest {

    private String rawContent;
    private String scanFormat;
    private String operatorName;
    private String sourceDevice;
    private String scannerInterface;
    private String scannerDeviceId;
}
