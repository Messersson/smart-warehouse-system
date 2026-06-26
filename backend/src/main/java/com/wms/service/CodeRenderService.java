package com.wms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.wms.common.BusinessException;
import com.wms.dto.CodeRenderRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class CodeRenderService {

    public Map<String, Object> render(CodeRenderRequest request) {
        String content = request == null ? null : request.getRawContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException("码内容不能为空");
        }

        String scanFormat = request == null ? null : request.getScanFormat();
        Integer requestedWidth = request == null ? null : request.getWidth();
        Integer requestedHeight = request == null ? null : request.getHeight();
        BarcodeFormat format = resolveFormat(scanFormat, content.trim());
        int width = normalizeSize(requestedWidth, format == BarcodeFormat.QR_CODE ? 220 : 420);
        int height = normalizeSize(requestedHeight, format == BarcodeFormat.QR_CODE ? width : 140);
        String svg = buildSvg(content.trim(), format, width, height);

        return Map.of(
                "scanFormat", format.name(),
                "rawContent", content.trim(),
                "svg", svg
        );
    }

    private BarcodeFormat resolveFormat(String scanFormat, String content) {
        if (scanFormat == null || scanFormat.isBlank()) {
            return BarcodeFormat.QR_CODE;
        }
        String normalized = scanFormat.trim().toUpperCase();
        return switch (normalized) {
            case "BAR_CODE", "BARCODE" -> isCode39Compatible(content) ? BarcodeFormat.CODE_39 : BarcodeFormat.CODE_128;
            case "CODE_39" -> BarcodeFormat.CODE_39;
            case "CODE_128" -> BarcodeFormat.CODE_128;
            case "QR_CODE", "QRCODE", "AUTO" -> BarcodeFormat.QR_CODE;
            default -> BarcodeFormat.QR_CODE;
        };
    }

    private boolean isCode39Compatible(String content) {
        return content != null && content.matches("[0-9A-Z .$/+%-]+");
    }

    private int normalizeSize(Integer value, int fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return Math.max(96, Math.min(value, 720));
    }

    private String buildSvg(String content, BarcodeFormat format, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, format == BarcodeFormat.QR_CODE ? 2 : 8);
            BitMatrix matrix = new MultiFormatWriter().encode(content, format, width, height, hints);
            return toSvg(matrix);
        } catch (Exception exception) {
            throw new BusinessException("生成码图形失败: " + exception.getMessage());
        }
    }

    private String toSvg(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        StringBuilder paths = new StringBuilder();
        for (int y = 0; y < height; y++) {
            int startX = -1;
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    if (startX < 0) {
                        startX = x;
                    }
                } else if (startX >= 0) {
                    appendRun(paths, startX, y, x - startX);
                    startX = -1;
                }
            }
            if (startX >= 0) {
                appendRun(paths, startX, y, width - startX);
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height
                + "\" viewBox=\"0 0 " + width + " " + height + "\" role=\"img\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                + "<path fill=\"#111827\" d=\"" + paths + "\"/>"
                + "</svg>";
    }

    private void appendRun(StringBuilder paths, int x, int y, int width) {
        paths.append('M').append(x).append(' ').append(y)
                .append("h").append(width)
                .append("v1H").append(x)
                .append('z');
    }
}
