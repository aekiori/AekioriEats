package com.delivery.user.config;

import com.delivery.user.exception.UnprocessableEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Value("${user.event.consumer.retry-attempts:3}")
    private long retryAttempts;

    @Value("${user.event.consumer.retry-interval-ms:1000}")
    private long retryIntervalMs;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory,
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        CommonErrorHandler errorHandler = buildErrorHandler(kafkaTemplate);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    private CommonErrorHandler buildErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 실패 메시지를 {원본토픽}.DLT 토픽으로 전송

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (ConsumerRecord<?, ?> record, Exception ex) ->
                new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", -1)
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer,
            // 총 시도수. retry 는 3번이라, 최초 1번 + 3번 = 총 4번
            new FixedBackOff(retryIntervalMs, retryAttempts)
        );

        // 재시도 없이 즉시 DLQ로
        errorHandler.addNotRetryableExceptions(UnprocessableEventException.class);

        return errorHandler;
    }
}
