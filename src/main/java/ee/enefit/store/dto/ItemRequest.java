package ee.enefit.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ItemRequest {
    private String name;
    private BigDecimal price;
    private int quantity;
}
