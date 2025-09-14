package ee.enefit.store.controller;

import ee.enefit.store.dto.SoldItemAggregateDto;
import ee.enefit.store.dto.StockLevelViewDto;
import ee.enefit.store.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/stock")
    public ResponseEntity<List<StockLevelViewDto>> getStockLevels() {
        return ResponseEntity.ok(reportService.getCurrentStockLevels());
    }

    @GetMapping("/sales/summary")
    public ResponseEntity<List<SoldItemAggregateDto>> getSoldItemsSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant fromInstant = (from == null)
                ? null
                : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = (to == null)
                ? null
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        List<SoldItemAggregateDto> result = reportService.getSoldItemsSummary(fromInstant, toInstant);
        return ResponseEntity.ok(result);
    }
}
