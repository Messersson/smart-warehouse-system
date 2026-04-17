package com.wms.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.serializers(
                    new LocalDateSerializer(DATE_FORMATTER),
                    new LocalDateTimeSerializer(DATE_TIME_FORMATTER)
            );
            builder.deserializers(
                    new FlexibleLocalDateDeserializer(),
                    new FlexibleLocalDateTimeDeserializer()
            );
        };
    }

    private static class FlexibleLocalDateDeserializer extends LocalDateDeserializer {

        private FlexibleLocalDateDeserializer() {
            super(DATE_FORMATTER);
        }

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (value == null || value.isBlank()) {
                return null;
            }
            String text = value.trim();
            try {
                return LocalDate.parse(text, DATE_FORMATTER);
            } catch (DateTimeParseException ignore) {
                try {
                    return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException exception) {
                    throw new IOException(exception);
                }
            }
        }
    }

    private static class FlexibleLocalDateTimeDeserializer extends LocalDateTimeDeserializer {

        private FlexibleLocalDateTimeDeserializer() {
            super(DATE_TIME_FORMATTER);
        }

        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (value == null || value.isBlank()) {
                return null;
            }
            String text = value.trim();
            try {
                return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignore) {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }
    }

}
