package ee.enefit.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ItemResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
    private int quantity;
}
