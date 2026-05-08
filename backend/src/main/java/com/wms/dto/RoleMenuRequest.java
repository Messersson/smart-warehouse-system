package com.wms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleMenuRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private List<Long> menuIds;
}
