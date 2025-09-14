package ee.enefit.store.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic itemsSoldTopic(@Value("${app.topics.items-sold}") String topic) {
        return new NewTopic(topic, 1, (short) 1);
    }
}
