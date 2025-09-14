package ee.enefit.store.messaging;

import java.util.UUID;

public interface SoldItemSummary {
    UUID getItemId();
    int getTotalSold();
}
