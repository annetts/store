package ee.enefit.store.service;

import ee.enefit.store.dto.ItemRequest;
import ee.enefit.store.dto.ItemResponse;
import ee.enefit.store.dto.ItemUpdateRequest;
import ee.enefit.store.entity.ItemEntity;
import ee.enefit.store.messaging.ItemSoldEvent;
import ee.enefit.store.messaging.ItemSoldProducer;
import ee.enefit.store.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    ItemRepository itemRepository;

    @Mock
    ItemSoldProducer producer;

    @InjectMocks
    ItemService service;

    UUID id;
    ItemEntity entity;

    @BeforeEach
    void setUp() {
        id = randomUUID();
        entity = new ItemEntity();
        entity.setId(id);
        entity.setName("Laptop");
        entity.setPrice(new BigDecimal("150.00"));
        entity.setQuantity(10);
        entity.setVersion(1);
    }

    @Test
    void getItemById_returnsMappedResponse_whenPresent() {
        when(itemRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<ItemResponse> out = service.getItemById(id);

        assertThat(out).isPresent();
        assertThat(out.get().getId()).isEqualTo(id);
        assertThat(out.get().getName()).isEqualTo("Laptop");
        assertThat(out.get().getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(out.get().getQuantity()).isEqualTo(10);
    }

    @Test
    void getItemById_returnsEmpty_whenNotFound() {
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ItemResponse> out = service.getItemById(id);

        assertThat(out).isEmpty();
    }

    @Test
    void createItem_persists_andReturnsMappedResponse() {
        ItemRequest req = new ItemRequest();
        req.setName("Mouse");
        req.setPrice(new BigDecimal("19.99"));
        req.setQuantity(5);

        ArgumentCaptor<ItemEntity> captor = ArgumentCaptor.forClass(ItemEntity.class);

        ItemEntity saved = new ItemEntity();
        UUID newId = randomUUID();
        saved.setId(newId);
        saved.setName("Mouse");
        saved.setPrice(new BigDecimal("19.99"));
        saved.setQuantity(5);
        saved.setVersion(1);

        when(itemRepository.save(any(ItemEntity.class))).thenReturn(saved);

        ItemResponse out = service.createItem(req);

        verify(itemRepository).save(captor.capture());
        ItemEntity toSave = captor.getValue();
        assertThat(toSave.getName()).isEqualTo("Mouse");
        assertThat(toSave.getPrice()).isEqualByComparingTo("19.99");
        assertThat(toSave.getQuantity()).isEqualTo(5);
        assertThat(toSave.getVersion()).isEqualTo(1);

        assertThat(out.getId()).isEqualTo(newId);
        assertThat(out.getName()).isEqualTo("Mouse");
        assertThat(out.getPrice()).isEqualByComparingTo("19.99");
        assertThat(out.getQuantity()).isEqualTo(5);
    }

    @Test
    void findByName_maps_whenFound() {
        when(itemRepository.findByNameIgnoreCase("laptop")).thenReturn(Optional.of(entity));

        Optional<ItemResponse> out = service.findByName("laptop");

        assertThat(out).isPresent();
        assertThat(out.get().getName()).isEqualTo("Laptop");
    }

    @Test
    void findByName_empty_whenMissing() {
        when(itemRepository.findByNameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThat(service.findByName("ghost")).isEmpty();
    }


    @Test
    void searchByName_mapsList() {
        ItemEntity e1 = copyOf(entity);
        ItemEntity e2 = copyOf(entity);
        e2.setId(randomUUID());
        e2.setName("Lapdesk");
        when(itemRepository.findByNameContainingIgnoreCase("lap"))
                .thenReturn(List.of(e1, e2));

        List<ItemResponse> out = service.searchByName("lap");

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getName()).isEqualTo("Laptop");
        assertThat(out.get(1).getName()).isEqualTo("Lapdesk");
    }

    @Test
    void searchByName_emptyList_whenNoMatches() {
        when(itemRepository.findByNameContainingIgnoreCase("zzz"))
                .thenReturn(List.of());

        assertThat(service.searchByName("zzz")).isEmpty();
    }


    @Test
    void deleteItem_returnsTrue_whenRepositoryDoesNotThrow() {
        doNothing().when(itemRepository).deleteById(id);

        assertThat(service.deleteItem(id)).isTrue();
        verify(itemRepository).deleteById(id);
    }

    @Test
    void deleteItem_returnsFalse_onEmptyResult() {
        doThrow(new EmptyResultDataAccessException(1))
                .when(itemRepository).deleteById(id);

        assertThat(service.deleteItem(id)).isFalse();
    }

    @Test
    void updateItem_updatesFields_andReturnsMappedResponse() {
        ItemEntity db = copyOf(entity);
        when(itemRepository.findById(id)).thenReturn(Optional.of(db));
        when(itemRepository.existsByNameIgnoreCase("NewName")).thenReturn(false);

        ItemEntity afterSave = copyOf(db);
        afterSave.setName("NewName");
        afterSave.setPrice(new BigDecimal("12.50"));
        afterSave.setQuantity(7);
        when(itemRepository.save(any(ItemEntity.class))).thenReturn(afterSave);

        ItemUpdateRequest req = new ItemUpdateRequest();
        req.setName("NewName");
        req.setPrice(12.50);
        req.setQuantity(7);

        Optional<ItemResponse> out = service.updateItem(id, req);

        assertThat(out).isPresent();
        assertThat(out.get().getName()).isEqualTo("NewName");
        assertThat(out.get().getPrice()).isEqualByComparingTo("12.50");
        assertThat(out.get().getQuantity()).isEqualTo(7);
        verify(itemRepository).save(argThat(e ->
                e.getName().equals("NewName")
                        && e.getPrice().compareTo(new BigDecimal("12.50")) == 0
                        && e.getQuantity() == 7
                        && e.getUpdatedAt() != null
        ));
    }

    @Test
    void updateItem_returnsEmpty_whenIdNotFound() {
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        ItemUpdateRequest req = new ItemUpdateRequest();
        req.setName("Anything");

        assertThat(service.updateItem(id, req)).isEmpty();
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_throws_whenNameConflictWithOther() {
        ItemEntity db = copyOf(entity);
        when(itemRepository.findById(id)).thenReturn(Optional.of(db));
        when(itemRepository.existsByNameIgnoreCase("Taken")).thenReturn(true);

        ItemUpdateRequest req = new ItemUpdateRequest();
        req.setName("Taken");

        assertThatThrownBy(() -> service.updateItem(id, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name already exists");
        verify(itemRepository, never()).save(any());
    }

    @Test
    void sell_happyPath_decrementsStock_saves_publishes_andReturnsEvent() {
        ItemEntity db = copyOf(entity);
        db.setQuantity(10);
        when(itemRepository.findById(id)).thenReturn(Optional.of(db));
        when(itemRepository.saveAndFlush(any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ItemSoldEvent> evtCaptor = ArgumentCaptor.forClass(ItemSoldEvent.class);

        ItemSoldEvent evt = service.sell(id, 2);

        verify(itemRepository).saveAndFlush(argThat(e -> e.getQuantity() == 8));

        verify(producer).publish(evtCaptor.capture());
        ItemSoldEvent published = evtCaptor.getValue();
        assertThat(published).isNotNull();
        assertThat(published.itemId()).isEqualTo(id);
        assertThat(published.quantity()).isEqualTo(2);
        assertThat(published.total()).isEqualByComparingTo(new BigDecimal("300.00"));

        assertThat(evt.itemId()).isEqualTo(id);
        assertThat(evt.total()).isEqualByComparingTo("300.00");
    }

    @Test
    void sell_throws_whenQuantityNotPositive() {
        assertThatThrownBy(() -> service.sell(id, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be > 0");
        verifyNoInteractions(itemRepository, producer);
    }

    @Test
    void sell_throws_whenItemNotFound() {
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sell(id, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item not found");
        verify(itemRepository, never()).saveAndFlush(any());
        verifyNoInteractions(producer);
    }

    @Test
    void sell_throws_whenInsufficientStock() {
        ItemEntity db = copyOf(entity);
        db.setQuantity(1);
        when(itemRepository.findById(id)).thenReturn(Optional.of(db));

        assertThatThrownBy(() -> service.sell(id, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
        verify(itemRepository, never()).saveAndFlush(any());
        verifyNoInteractions(producer);
    }

    @Test
    void sell_wrapsOptimisticLock_asIllegalState() {
        ItemEntity db = copyOf(entity);
        when(itemRepository.findById(id)).thenReturn(Optional.of(db));
        doThrow(new OptimisticLockingFailureException("boom"))
                .when(itemRepository).saveAndFlush(any(ItemEntity.class));

        assertThatThrownBy(() -> service.sell(id, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Concurrent update detected");
        verifyNoInteractions(producer);
    }

    @Test
    void isNameConflict_false_whenNullOrBlank() {
        assertThat(service.isNameConflict(id, null)).isFalse();
        assertThat(service.isNameConflict(id, "")).isFalse();
        assertThat(service.isNameConflict(id, "   ")).isFalse();
    }

    @Test
    void isNameConflict_false_whenSameId() {
        ItemEntity same = copyOf(entity);
        when(itemRepository.findByNameIgnoreCase("Laptop")).thenReturn(Optional.of(same));

        assertThat(service.isNameConflict(id, "Laptop")).isFalse();
    }

    @Test
    void isNameConflict_true_whenDifferentIdHasName() {
        ItemEntity other = copyOf(entity);
        other.setId(randomUUID());
        when(itemRepository.findByNameIgnoreCase("Laptop")).thenReturn(Optional.of(other));

        assertThat(service.isNameConflict(id, "Laptop")).isTrue();
    }

    private static ItemEntity copyOf(ItemEntity src) {
        ItemEntity e = new ItemEntity();
        e.setId(src.getId());
        e.setName(src.getName());
        e.setPrice(src.getPrice());
        e.setQuantity(src.getQuantity());
        e.setVersion(src.getVersion());
        e.setUpdatedAt(src.getUpdatedAt());
        return e;
    }
}