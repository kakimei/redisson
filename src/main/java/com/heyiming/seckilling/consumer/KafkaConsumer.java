package com.heyiming.seckilling.consumer;

import com.heyiming.seckilling.service.SeckillingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class KafkaConsumer {

    private Consumer<String, String> stringConsumer = null;

    @Value("#{environment['kafka.topic']}")
    private String TOPIC;

    @Value("#{environment['kafka.bootstrap.servers']}")
    private String BOOT_STRAP_SERVERS;

    @PostConstruct
    public void init(){
        Properties kafkaProperties = new Properties();
        kafkaProperties.put("bootstrap.servers", BOOT_STRAP_SERVERS);
        kafkaProperties.put("group.id", "ReserveGroup");
        kafkaProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        stringConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(kafkaProperties);
        stringConsumer.subscribe(Collections.singletonList(TOPIC));
        process();
    }

    @Autowired
    private SeckillingService seckillingService;

    private void process(){
        new Thread(() -> {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = stringConsumer.poll(Duration.ofMinutes(1));
                    for (ConsumerRecord consumerRecord : records) {
                        log.debug("topic = {}, partition = {}, offset = {}, customer = {}, country = {}",
                                consumerRecord.topic(),
                                consumerRecord.partition(),
                                consumerRecord.offset(),
                                consumerRecord.key(),
                                consumerRecord.value());
                        log.info("kafka message is : {}", consumerRecord.value());
                        seckillingService.reserve(Stream.of(String.valueOf(consumerRecord.value())).collect(Collectors.toList()), "111");
                    }
                }
            }finally {
                stringConsumer.close();
            }
        }).start();
    }
}
