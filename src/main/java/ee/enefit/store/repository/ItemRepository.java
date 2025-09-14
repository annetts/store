package ee.enefit.store.repository;

import ee.enefit.store.dto.StockLevelViewDto;
import ee.enefit.store.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {
    Optional<ItemEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<ItemEntity> findByNameContainingIgnoreCase(String namePart);

    @Query("""
           select
             i.id   as id,
             i.name as name,
             i.price as price,
             i.quantity as stockQuantity,
             i.updatedAt as lastUpdated
           from ItemEntity i
           order by i.name
           """)
    List<StockLevelViewDto> findCurrentStockLevels();
}
