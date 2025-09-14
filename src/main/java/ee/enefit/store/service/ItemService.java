package ee.enefit.store.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ee.enefit.store.dto.ItemRequest;
import ee.enefit.store.dto.ItemUpdateRequest;
import ee.enefit.store.entity.ItemEntity;
import ee.enefit.store.messaging.ItemSoldEvent;
import ee.enefit.store.messaging.ItemSoldProducer;
import ee.enefit.store.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import ee.enefit.store.dto.ItemResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemSoldProducer producer;

    public Optional<ItemResponse> getItemById(UUID id) {
        return itemRepository.findById(id)
                .map(this::mapToResponse);
    }

    public ItemResponse createItem(ItemRequest request) {
        ItemEntity entity = new ItemEntity();
        entity.setName(request.getName());
        entity.setPrice(request.getPrice());
        entity.setQuantity(request.getQuantity());
        entity.setVersion(1);

        ItemEntity saved = itemRepository.save(entity);
        return mapToResponse(saved);
    }

    public Optional<ItemResponse> findByName(String name) {
        return itemRepository.findByNameIgnoreCase(name)
                .map(this::mapToResponse);
    }

    public List<ItemResponse> searchByName(String name) {
        return itemRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<ItemResponse> updateItem(UUID id, ItemUpdateRequest request) {
        Optional<ItemEntity> optionalEntity = itemRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            return Optional.empty();
        }

        ItemEntity entity = optionalEntity.get();

        if (request.getName() != null &&
                !request.getName().equalsIgnoreCase(entity.getName()) &&
                itemRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Name already exists");
        }
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getPrice() != null) {
            entity.setPrice(BigDecimal.valueOf(request.getPrice()));
        }
        if (request.getQuantity() != null) {
            entity.setQuantity(request.getQuantity());
        }
        entity.setUpdatedAt(OffsetDateTime.now().toInstant());
        ItemEntity updated = itemRepository.save(entity);
        return Optional.of(mapToResponse(updated));
    }

    @Transactional
    public ItemSoldEvent sell(UUID itemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (item.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock. Requested " + quantity + ", available "
                    + item.getQuantity());
        }
        item.setQuantity(item.getQuantity() - quantity);
        try {
            itemRepository.saveAndFlush(item);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Concurrent update detected for item " + itemId);
        }
        BigDecimal total = item.getPrice().multiply(BigDecimal.valueOf(quantity));
        ItemSoldEvent event = new ItemSoldEvent(
                UUID.randomUUID(),
                itemId,
                quantity,
                item.getPrice(),
                total,
                Instant.now()
        );
        producer.publish(event);
        return event;
    }

    public boolean isNameConflict(UUID id, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        Optional<ItemEntity> existing = itemRepository.findByNameIgnoreCase(name);
        return existing.isPresent() && !existing.get().getId().equals(id);
    }

    private ItemResponse mapToResponse(ItemEntity entity) {
        return new ItemResponse(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getQuantity()
        );
    }
}