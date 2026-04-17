package com.wms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardOverviewResponse {

    private long warehouseCount;
    private long productCount;
    private long pendingInboundCount;
    private long pendingOutboundCount;
    private long unresolvedAlertCount;
    private BigDecimal totalStockQuantity;
    private List<Map<String, Object>> recentInbounds;
    private List<Map<String, Object>> recentOutbounds;
    private List<Map<String, Object>> hotAlerts;
}
