package com.heyiming.seckilling.consumer;

import com.heyiming.seckilling.service.SeckillingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class KafkaConsumer {

    private Consumer<String, String> stringConsumer = null;

    @Value("#{environment['kafka.topic']}")
    private String TOPIC;

    @Value("#{environment['kafka.bootstrap.servers']}")
    private String BOOT_STRAP_SERVERS;

    private static final Pattern MESSAGE_PATTEN = Pattern.compile("(?<itemName>\\w+)\\|(?<memberSrl>\\w+)");
    @PostConstruct
    public void init(){
        Properties kafkaProperties = new Properties();
        kafkaProperties.put("bootstrap.servers", BOOT_STRAP_SERVERS);
        kafkaProperties.put("group.id", "ReserveGroup");
        kafkaProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProperties.put("fetch.min.bytes", 5000);
        kafkaProperties.put("fetch.max.wait.ms", 100);
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
                    Map<String, List<String>> itemMemberSrlMapping = new HashMap<>();
                    for (ConsumerRecord consumerRecord : records) {
                        log.debug("topic = {}, partition = {}, offset = {}, customer = {}, country = {}",
                                consumerRecord.topic(),
                                consumerRecord.partition(),
                                consumerRecord.offset(),
                                consumerRecord.key(),
                                consumerRecord.value());
                        log.info("kafka message is : {}", consumerRecord.value());
                        // itemName|memberSrl
                        String message = (String)consumerRecord.value();
                        Matcher matcher = MESSAGE_PATTEN.matcher(message);
                        String itemName = null;
                        String memberSrl = null;
                        if(matcher.find()){
                            itemName = matcher.group("itemName");
                            memberSrl = matcher.group("memberSrl");
                        }
                        log.info("itemName: {}, memberSrl: {}", itemName, memberSrl);
                        if(StringUtils.isEmpty(itemName) || StringUtils.isEmpty(memberSrl)){
                            log.warn("message {} format is not valid.", message);
                            continue;
                        }
                        List<String> memberList = itemMemberSrlMapping.get(itemName);
                        if(memberList == null){
                            memberList = new ArrayList<>();
                            memberList.add(memberSrl);
                            itemMemberSrlMapping.put(itemName, memberList);
                        }else{
                            memberList.add(memberSrl);
                        }
                    }
                    tryReserve(itemMemberSrlMapping);

                }
            }finally {
                stringConsumer.close();
            }
        }).start();
    }

    private void tryReserve(Map<String, List<String>> itemMemberSrlMapping){
        for(Map.Entry<String, List<String>> entry : itemMemberSrlMapping.entrySet()){
            seckillingService.reserve(entry.getValue(), entry.getKey());
        }
    }
}
