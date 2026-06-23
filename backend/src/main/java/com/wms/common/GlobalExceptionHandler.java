package com.wms.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Pattern DUPLICATE_ENTRY_PATTERN = Pattern.compile("Duplicate entry '(.+?)' for key '(.+?)'");
    private static final Map<String, String> DUPLICATE_KEY_LABELS = Map.ofEntries(
            Map.entry("base_warehouse.warehouse_code", "\u4ed3\u5e93\u7f16\u7801"),
            Map.entry("uk_location_code", "\u5e93\u4f4d\u7f16\u7801"),
            Map.entry("base_owner.owner_code", "\u8d27\u4e3b\u7f16\u7801"),
            Map.entry("base_supplier.supplier_code", "\u4f9b\u5e94\u5546\u7f16\u7801"),
            Map.entry("base_customer.customer_code", "\u5ba2\u6237\u7f16\u7801"),
            Map.entry("base_product.sku_code", "SKU\u7f16\u7801"),
            Map.entry("sys_user.username", "\u7528\u6237\u540d"),
            Map.entry("sys_role.role_code", "\u89d2\u8272\u7f16\u7801"),
            Map.entry("sys_menu.menu_code", "\u83dc\u5355\u7f16\u7801")
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("\u8bf7\u6c42\u53c2\u6570\u6821\u9a8c\u5931\u8d25");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(resolveDataIntegrityMessage(exception)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("\u8bf7\u6c42\u6570\u636e\u683c\u5f0f\u4e0d\u6b63\u786e\uff0c\u8bf7\u68c0\u67e5\u65e5\u671f\u3001\u6570\u5b57\u7b49\u5b57\u6bb5"));
    }

    @ExceptionHandler({NoSuchElementException.class, EmptyResultDataAccessException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("\u8bb0\u5f55\u4e0d\u5b58\u5728\u6216\u5df2\u88ab\u5220\u9664"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("\u8bf7\u6c42\u8d44\u6e90\u4e0d\u5b58\u5728"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled system exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("\u7cfb\u7edf\u5f02\u5e38\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5e76\u63d0\u4f9b\u8bf7\u6c42\u7f16\u53f7"));
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException exception) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
        String message = rootCause == null ? exception.getMessage() : rootCause.getMessage();
        if (message == null || message.isBlank()) {
            return "\u6570\u636e\u7ea6\u675f\u6821\u9a8c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u91cd\u8bd5";
        }

        Matcher matcher = DUPLICATE_ENTRY_PATTERN.matcher(message);
        if (matcher.find()) {
            String value = matcher.group(1);
            String key = matcher.group(2);
            String fieldLabel = DUPLICATE_KEY_LABELS.getOrDefault(key, "\u5173\u952e\u5b57\u6bb5");
            return fieldLabel + "\u5df2\u5b58\u5728: " + value;
        }

        return "\u6570\u636e\u7ea6\u675f\u6821\u9a8c\u5931\u8d25: " + message;
    }
}
