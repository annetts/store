package ee.enefit.store.controller;

import ee.enefit.store.messaging.ItemSoldEvent;
import ee.enefit.store.service.ItemService;
import ee.enefit.store.dto.ItemRequest;
import ee.enefit.store.dto.ItemResponse;
import ee.enefit.store.dto.ItemUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    public record SellRequest(@Min(1) int quantity) {}

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@RequestBody ItemRequest request) {
        if (itemService.findByName(request.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        ItemResponse created = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable UUID id) {
        return itemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(params = "name")
    public ResponseEntity<List<ItemResponse>> getItemsByName(@RequestParam String name) {
        List<ItemResponse> items = itemService.searchByName(name);
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable("id") UUID id) {
        boolean deleted = itemService.deleteItem(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable UUID id,
            @RequestBody ItemUpdateRequest request
    ) {
        Optional<ItemResponse> updated = itemService.updateItem(id, request);
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (itemService.isNameConflict(id, request.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok(updated.get());
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<ItemSoldEvent> sell(@PathVariable("id") UUID id,
                                              @Valid @RequestBody SellRequest body) {
        ItemSoldEvent event = itemService.sell(id, body.quantity());
        return ResponseEntity.ok(event);
    }
}
