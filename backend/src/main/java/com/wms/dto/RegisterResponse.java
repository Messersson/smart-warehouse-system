package com.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {

    private Long userId;
    private String username;
    private String displayName;
    private String roleCode;
    private String status;
    private String message;
}
