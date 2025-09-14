package ee.enefit.store.messaging;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ItemSoldProducer {

    private final KafkaTemplate<String, ItemSoldEvent> kafkaTemplate;

    @Value("${app.topics.items-sold}")
    private String topic;

    public CompletableFuture<SendResult<String, ItemSoldEvent>> publish(ItemSoldEvent event) {
        String key = event.itemId().toString();
        CompletableFuture<SendResult<String, ItemSoldEvent>> future = kafkaTemplate.send(topic, key, event);
        return future.whenComplete((res, ex) -> {
            if (ex != null) {
                // You can log/metric here
            } else {
                RecordMetadata m = res.getRecordMetadata();
                // Optional log: topic, partition, offset
            }
        });
    }
}
