package com.inventario.multisucursal.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * docs/API_DESIGN.md, sección 7.10. Sin {@code @PreAuthorize}: los tres roles
 * pueden consultar el reporte (UC-12), y la restricción real —un
 * {@code OPERATOR} solo ve su propia sucursal— depende del parámetro pedido,
 * así que la aplica {@link LogisticsComplianceService}.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final LogisticsComplianceService logisticsComplianceService;

    public ReportController(LogisticsComplianceService logisticsComplianceService) {
        this.logisticsComplianceService = logisticsComplianceService;
    }

    @GetMapping("/logistics-compliance")
    public LogisticsComplianceResponse logisticsCompliance(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo) {
        return logisticsComplianceService.report(branchId, routeId, dateFrom, dateTo);
    }
}
