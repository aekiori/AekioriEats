package com.delivery.user.service.kafkadlt;

import com.delivery.user.dto.request.KafkaDltReadRequestDto;
import com.delivery.user.dto.request.KafkaDltReplayRequestDto;
import com.delivery.user.dto.response.KafkaDltHeaderResponseDto;
import com.delivery.user.dto.response.KafkaDltMessageResponseDto;
import com.delivery.user.dto.response.KafkaDltReplayResponseDto;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaDltService {
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final int MAX_EMPTY_POLLS = 3;
    private static final String DLT_SUFFIX = ".DLT";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String bootstrapServers;

    public KafkaDltService(
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.bootstrapServers = bootstrapServers;
    }

    public List<KafkaDltMessageResponseDto> readMessages(KafkaDltReadRequestDto request) {
        String topic = request.topic();
        int partition = request.resolvedPartition();
        long offset = request.resolvedOffset();
        int limit = request.resolvedLimit();
        validateDltTopic(topic);

        TopicPartition topicPartition = new TopicPartition(topic, partition);

        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.assign(List.of(topicPartition));
            consumer.seek(topicPartition, offset);

            List<KafkaDltMessageResponseDto> messages = new ArrayList<>();
            int emptyPolls = 0;

            while (messages.size() < limit && emptyPolls < MAX_EMPTY_POLLS) {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }

                for (ConsumerRecord<String, String> record : records.records(topicPartition)) {
                    messages.add(toResponse(record));
                    if (messages.size() >= limit) {
                        break;
                    }
                }
            }

            return messages;
        }
    }

    public KafkaDltReplayResponseDto replay(KafkaDltReplayRequestDto request) {
        validateDltTopic(request.topic());

        ConsumerRecord<String, String> record = readRecord(request.topic(), request.partition(), request.offset());
        String targetTopic = resolveTargetTopic(request.topic(), request.targetTopic());
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(targetTopic, record.key(), record.value());

        copyReplayHeaders(record, producerRecord);
        send(producerRecord);

        return new KafkaDltReplayResponseDto(
            request.topic(),
            request.partition(),
            request.offset(),
            targetTopic,
            record.key()
        );
    }

    private ConsumerRecord<String, String> readRecord(String topic, int partition, long offset) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);

        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.assign(List.of(topicPartition));
            consumer.seek(topicPartition, offset);

            int emptyPolls = 0;
            while (emptyPolls < MAX_EMPTY_POLLS) {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }

                for (ConsumerRecord<String, String> record : records.records(topicPartition)) {
                    if (record.offset() == offset) {
                        return record;
                    }
                    if (record.offset() > offset) {
                        break;
                    }
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DLT record not found.");
    }

    private KafkaConsumer<String, String> newConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "user-dlt-admin-" + UUID.randomUUID());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private void send(ProducerRecord<String, String> producerRecord) {
        try {
            kafkaTemplate.send(producerRecord).get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to replay DLT record.", exception);
        }
    }

    private void validateDltTopic(String topic) {
        if (topic == null || topic.isBlank() || !topic.endsWith(DLT_SUFFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DLT topic must end with .DLT.");
        }
    }

    private String resolveTargetTopic(String sourceDltTopic, String requestedTargetTopic) {
        String targetTopic = requestedTargetTopic == null || requestedTargetTopic.isBlank()
            ? sourceDltTopic.substring(0, sourceDltTopic.length() - DLT_SUFFIX.length())
            : requestedTargetTopic.trim();

        if (targetTopic.endsWith(DLT_SUFFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target topic must not be a DLT topic.");
        }

        return targetTopic;
    }

    private KafkaDltMessageResponseDto toResponse(ConsumerRecord<String, String> record) {
        return new KafkaDltMessageResponseDto(
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            record.value(),
            Instant.ofEpochMilli(record.timestamp()).toString(),
            toHeaders(record)
        );
    }

    private List<KafkaDltHeaderResponseDto> toHeaders(ConsumerRecord<String, String> record) {
        List<KafkaDltHeaderResponseDto> headers = new ArrayList<>();
        for (Header header : record.headers()) {
            headers.add(new KafkaDltHeaderResponseDto(
                header.key(),
                header.value() == null ? null : Base64.getEncoder().encodeToString(header.value())
            ));
        }
        return headers;
    }

    private void copyReplayHeaders(ConsumerRecord<String, String> source, ProducerRecord<String, String> target) {
        for (Header header : source.headers()) {
            if (!header.key().startsWith("kafka_dlt")) {
                target.headers().add(header.key(), header.value());
            }
        }
    }

}
