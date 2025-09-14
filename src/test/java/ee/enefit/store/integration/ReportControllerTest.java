package ee.enefit.store.integration;

import ee.enefit.store.controller.ReportController;
import ee.enefit.store.dto.SoldItemAggregateDto;
import ee.enefit.store.dto.StockLevelViewDto;
import ee.enefit.store.service.ReportService;
;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = ReportController.class)
class ReportControllerTest {
    @MockitoBean
    private ReportService reportService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getStockLevels_returnsOkAndBody() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<StockLevelViewDto> mockData = List.of(
                new StockLevelViewDto(id1, "Apple", new BigDecimal("1.99"), 10, Instant.parse("2025-09-10T12:00:00Z")),
                new StockLevelViewDto(id2, "Banana", new BigDecimal("2.49"), 0,  Instant.parse("2025-09-11T12:00:00Z"))
        );

        when(reportService.getCurrentStockLevels()).thenReturn(mockData);

        mockMvc.perform(get("/api/reports/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[0].stockQuantity").value(10))
                .andExpect(jsonPath("$[1].id").value(id2.toString()))
                .andExpect(jsonPath("$[1].name").value("Banana"))
                .andExpect(jsonPath("$[1].stockQuantity").value(0));
    }

    @Test
    void getSoldItemsSummary_withoutDates_returnsOk_andPassesNulls() throws Exception {
        when(reportService.getSoldItemsSummary(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/sales/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(reportService).getSoldItemsSummary(isNull(), isNull());
    }

    @Test
    void getSoldItemsSummary_withDateRange_returnsOk_andConvertsBounds() throws Exception {
        UUID itemId = UUID.randomUUID();
        Instant lastSoldAt = Instant.parse("2025-09-12T10:00:00Z");
        List<SoldItemAggregateDto> mock = List.of(
                new SoldItemAggregateDto(itemId, "Pen", 5L, new BigDecimal("9.95"), lastSoldAt)
        );
        when(reportService.getSoldItemsSummary(any(), any())).thenReturn(mock);

        mockMvc.perform(get("/api/reports/sales/summary")
                        .param("from", "2025-09-01")
                        .param("to", "2025-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(itemId.toString()))
                .andExpect(jsonPath("$[0].name").value("Pen"))
                .andExpect(jsonPath("$[0].unitsSold").value(5))
                .andExpect(jsonPath("$[0].revenue").value(9.95))
                .andExpect(jsonPath("$[0].lastSoldAt").value(lastSoldAt.toString()));

        ArgumentCaptor<Instant> fromCap = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCap = ArgumentCaptor.forClass(Instant.class);
        verify(reportService).getSoldItemsSummary(fromCap.capture(), toCap.capture());

        Instant expectedFrom = LocalDate.parse("2025-09-01")
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant expectedTo = LocalDate.parse("2025-09-14")
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .minusNanos(1);

        assertEquals(expectedFrom, fromCap.getValue());
        assertEquals(expectedTo, toCap.getValue());
    }
}
