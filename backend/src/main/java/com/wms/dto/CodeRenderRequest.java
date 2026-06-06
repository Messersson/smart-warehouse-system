package com.wms.dto;

import lombok.Data;

@Data
public class CodeRenderRequest {

    private String rawContent;
    private String scanFormat;
    private Integer width;
    private Integer height;
}
