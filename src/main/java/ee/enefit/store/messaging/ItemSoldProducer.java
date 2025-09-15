package ee.enefit.store.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemSoldProducer {
    private final KafkaTemplate<String, ItemSoldEvent> kafkaTemplate;

    @Value("${app.topics.items-sold}")
    private String topic;

    public void publish(ItemSoldEvent event) {
        String key = event.itemId().toString();
        CompletableFuture<SendResult<String, ItemSoldEvent>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("Failed to publish ItemSoldEvent with key={}: {}", key, ex.getMessage(), ex);
            } else {
                RecordMetadata m = res.getRecordMetadata();
                log.info("Successfully published ItemSoldEvent to topic={}, partition={}, offset={}",
                        m.topic(), m.partition(), m.offset());
            }
        });
    }
}
