// src/main/java/dev/bluemap/secom/provider/LocalDateTimeParamConverterProvider.java

package dev.bluemap.secom.provider;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Provider
@Component
public class LocalDateTimeParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType,
                                               Type genericType,
                                               Annotation[] annotations) {
        if (!rawType.equals(LocalDateTime.class)) return null;

        return (ParamConverter<T>) new ParamConverter<LocalDateTime>() {
            @Override
            public LocalDateTime fromString(String value) {
                if (value == null || value.isBlank()) return null;
                // Z 제거 후 파싱
                String v = value.endsWith("Z")
                        ? value.substring(0, value.length() - 1)
                        : value;
                return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            @Override
            public String toString(LocalDateTime value) {
                return value == null ? null : value.toString();
            }
        };
    }
}