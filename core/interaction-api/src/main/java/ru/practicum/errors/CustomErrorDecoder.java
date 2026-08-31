package ru.practicum.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception = defaultDecoder.decode(methodKey, response);
        String errorBody = "";
        try (InputStream body = response.body().asInputStream()) {
            errorBody = objectMapper.readValue(body, String.class);
        } catch (IOException e) {
            log.error("Failed to read error body", e);
        }
        log.error("Feign error: method={}, status={}, message={}, body={}", methodKey, response.status(), exception.getMessage(), errorBody);
        return exception;
    }
}
