package ru.practicum.errors;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            return defaultDecoder.decode(methodKey, response);
        } finally {
            try {
                response.body().close();
            } catch (Exception ignored) {
            }
        }
    }
}
