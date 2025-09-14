package ee.enefit.store.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockLevelViewDto(
        UUID id,
        String name,
        BigDecimal price,
        Integer stockQuantity,
        Instant lastUpdated
) {}
