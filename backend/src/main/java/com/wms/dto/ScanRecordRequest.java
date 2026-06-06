package com.wms.dto;

import lombok.Data;

@Data
public class ScanRecordRequest {

    private String rawContent;
    private String scanFormat;
    private String sourceDevice;
    private String scannerInterface;
    private String scannerDeviceId;
    private String operatorName;
    private String remark;
}
