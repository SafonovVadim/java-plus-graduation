package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.*;
import ru.practicum.ewm.stats.proto.*;


import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorService {
    private final KafkaProducer<String, byte[]> kafkaProducer;

    @Value("${kafka.topics.output}")
    private String userActionTopic;

    public void sendUserAction(UserActionProto userActionProto) {
        log.info("Получено действие: userId={}, eventId={}, actionType={}",
                userActionProto.getUserId(), userActionProto.getEventId(), userActionProto.getActionType());
        UserActionAvro userActionAvro = convertToAvro(userActionProto);
        byte[] avroData = serializeAvro(userActionAvro);

        kafkaProducer.send(new ProducerRecord<>(userActionTopic,
                String.valueOf(userActionProto.getUserId()), avroData),
                (metadata, exception) -> {
                    if (exception == null) {
                        log.info("Отправка сообщения в топик {} партицию {} офсет {}",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    } else {
                        log.error("Ошибка отправки действия пользователя", exception);
                    }
                });
    }

    private UserActionAvro convertToAvro(UserActionProto proto) {
        ActionTypeAvro actionTypeAvro = mapActionType(proto.getActionType());
        long timestamp = proto.getTimestamp().getSeconds() * 1000L +
                proto.getTimestamp().getNanos() / 1_000_000;

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(actionTypeAvro)
                .setTimestamp(timestamp)
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
