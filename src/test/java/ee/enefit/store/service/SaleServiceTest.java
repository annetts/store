package ee.enefit.store.service;

import ee.enefit.store.dto.SoldItemAggregateDto;
import ee.enefit.store.dto.StockLevelViewDto;
import ee.enefit.store.repository.ItemRepository;
import ee.enefit.store.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ItemRepository itemRepository;
    @Mock SaleRepository saleRepository;

    @InjectMocks ReportService reportService;

    StockLevelViewDto stockDto;
    SoldItemAggregateDto soldDto;

    @BeforeEach
    void setup() {

        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();

        stockDto = new StockLevelViewDto(
                itemId,
                "Laptop",
                new BigDecimal("150.00"),
                5,
                now
        );
        soldDto = new SoldItemAggregateDto(
                itemId,
                "Laptop",
                10L,
                new BigDecimal("1500.00"),
                now
        );
    }

    @Test
    void getCurrentStockLevels_returnsRepositoryResults() {
        when(itemRepository.findCurrentStockLevels())
                .thenReturn(List.of(stockDto));

        List<StockLevelViewDto> result = reportService.getCurrentStockLevels();

        assertThat(result).containsExactly(stockDto);
        verify(itemRepository).findCurrentStockLevels();
        verifyNoInteractions(saleRepository);
    }

    @Test
    void getSoldItemsSummary_usesProvidedBounds() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to   = Instant.parse("2024-12-31T23:59:59Z");

        when(saleRepository.findSoldItemsSummary(from, to))
                .thenReturn(List.of(soldDto));

        List<SoldItemAggregateDto> result = reportService.getSoldItemsSummary(from, to);

        assertThat(result).containsExactly(soldDto);
        verify(saleRepository).findSoldItemsSummary(from, to);
    }

    @Test
    void getSoldItemsSummary_defaultsWhenNull() {
        Instant expectedFrom = Instant.parse("1970-01-01T00:00:00Z");
        Instant expectedTo   = Instant.parse("9999-12-31T23:59:59Z");

        when(saleRepository.findSoldItemsSummary(expectedFrom, expectedTo))
                .thenReturn(List.of(soldDto));

        List<SoldItemAggregateDto> result = reportService.getSoldItemsSummary(null, null);

        assertThat(result).containsExactly(soldDto);
        verify(saleRepository).findSoldItemsSummary(expectedFrom, expectedTo);
    }
}
