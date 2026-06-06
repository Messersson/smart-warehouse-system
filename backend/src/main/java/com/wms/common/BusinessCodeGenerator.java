package com.wms.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class BusinessCodeGenerator {

    private static final DateTimeFormatter ORDER_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private BusinessCodeGenerator() {
    }

    public static String unifiedOrderCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.trim();
        }
        int sequence = SEQUENCE.updateAndGet(value -> value >= 999 ? 0 : value + 1);
        return "WMS" + ORDER_CODE_FORMATTER.format(LocalDateTime.now()) + String.format(Locale.ROOT, "%03d", sequence);
    }

    public static String productBarcode() {
        int sequence = SEQUENCE.updateAndGet(value -> value >= 999 ? 0 : value + 1);
        return "WMSP" + ORDER_CODE_FORMATTER.format(LocalDateTime.now()) + String.format(Locale.ROOT, "%03d", sequence);
    }

    public static String cargoCode() {
        int sequence = SEQUENCE.updateAndGet(value -> value >= 999 ? 0 : value + 1);
        return "WMSG" + ORDER_CODE_FORMATTER.format(LocalDateTime.now()) + String.format(Locale.ROOT, "%03d", sequence);
    }
}
