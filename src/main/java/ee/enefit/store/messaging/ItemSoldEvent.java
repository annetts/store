package ee.enefit.store.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemSoldEvent(
        UUID saleId,
        UUID itemId,
        int quantity,
        BigDecimal priceAtSale,
        BigDecimal total,
        Instant soldAt
) {}
