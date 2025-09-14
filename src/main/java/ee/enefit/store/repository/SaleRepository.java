package ee.enefit.store.repository;

import ee.enefit.store.dto.SoldItemAggregateDto;
import ee.enefit.store.entity.SaleEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<SaleEntity, UUID> {

    @Query("""
           select
             i.id            as itemId,
             i.name          as name,
             sum(s.quantity) as unitsSold,
             sum(s.total)    as revenue,
             max(s.soldAt)   as lastSoldAt
           from SaleEntity s, ItemEntity i
           where s.itemId = i.id
             and s.soldAt between :from and :to
           group by i.id, i.name
           order by sum(s.quantity) desc
           """)
    List<SoldItemAggregateDto> findSoldItemsSummary(
            @Param("from") Instant from,
            @Param("to")   Instant to
    );
}
