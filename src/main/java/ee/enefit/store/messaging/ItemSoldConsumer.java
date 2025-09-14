package ee.enefit.store.messaging;

import ee.enefit.store.entity.SaleEntity;
import ee.enefit.store.repository.SaleRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ItemSoldConsumer {

    private final SaleRepository saleRepository;

    @KafkaListener(topics = "${app.topics.items-sold}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handle(ItemSoldEvent event) {
        Optional<SaleEntity> existing = saleRepository.findById(event.saleId());
        if (existing.isPresent()) {
            return;
        }

        SaleEntity sale = SaleEntity.builder()
                .id(event.saleId())
                .itemId(event.itemId())
                .quantity(event.quantity())
                .priceAtSale(event.priceAtSale())
                .total(event.total())
                .soldAt(event.soldAt())
                .build();

        saleRepository.save(sale);
    }
}
