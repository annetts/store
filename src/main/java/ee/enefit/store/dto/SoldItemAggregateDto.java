package ee.enefit.store.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SoldItemAggregateDto(
        UUID itemId,
        String name,
        Long unitsSold,
        BigDecimal revenue,
        Instant lastSoldAt
) {}
