package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorService {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.output}")
    private String userActionTopic;

    public void sendUserAction(UserActionProto userActionProto) {
        log.info("Получено действие: userId={}, eventId={}, actionType={}",
                userActionProto.getUserId(), userActionProto.getEventId(), userActionProto.getActionType());
        UserActionAvro userActionAvro = convertToAvro(userActionProto);
        byte[] avroData = serializeAvro(userActionAvro);

        try {
            kafkaTemplate.send(userActionTopic, avroData)
                    .whenComplete((metadata, exception) -> {
                        if (exception == null) {
                            log.info("Отправка сообщения в топик {} партицию {} офсет {}",
                                    metadata.getRecordMetadata().topic(),
                                    metadata.getRecordMetadata().partition(),
                                    metadata.getRecordMetadata().offset());
                        } else {
                            log.error("Ошибка отправки действия пользователя", exception);
                        }
                    });
        } catch (Exception e) {
            log.error("Ошибка отправки действия пользователя", e);
        }
    }

    private UserActionAvro convertToAvro(UserActionProto proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(mapActionType(proto.getActionType()))
                .setTimestamp(java.time.Instant.ofEpochMilli(
                        proto.getTimestamp().getSeconds() * 1000
                                + proto.getTimestamp().getNanos() / 1_000_000))
                .build();
    }

    private ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
        };
    }

    private byte[] serializeAvro(UserActionAvro userActionAvro) {
        try {
            SpecificDatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.class);
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(userActionAvro, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка преобразования UserActionAvro", e);
        }
    }
}
