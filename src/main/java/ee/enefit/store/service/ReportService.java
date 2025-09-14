package ee.enefit.store.service;

import ee.enefit.store.dto.SoldItemAggregateDto;
import ee.enefit.store.dto.StockLevelViewDto;
import ee.enefit.store.repository.ItemRepository;
import ee.enefit.store.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ReportService {

    private final ItemRepository itemRepository;
    private final SaleRepository saleRepository;

    public List<StockLevelViewDto> getCurrentStockLevels() {
        return itemRepository.findCurrentStockLevels();
    }

    public List<SoldItemAggregateDto> getSoldItemsSummary(Instant from, Instant to) {
        Instant fromBound = (from != null)
                ? from
                : Instant.parse("1970-01-01T00:00:00Z");
        Instant toBound = (to != null)
                ? to
                : Instant.parse("9999-12-31T23:59:59Z");
        return saleRepository.findSoldItemsSummary(fromBound, toBound);
    }
}
