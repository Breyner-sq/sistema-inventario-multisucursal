package com.inventario.multisucursal.sales;

import com.inventario.multisucursal.auth.AuthenticatedUser;
import com.inventario.multisucursal.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** docs/API_DESIGN.md, sección 7.8. Lectura acotada a la propia sucursal salvo ADMIN (sección 6, igual que purchase-orders). */
@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return saleService.list(branchId, status, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable Long id) {
        return saleService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public SaleResponse create(
            @Valid @RequestBody CreateSaleRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return saleService.confirmSale(request, principal.userId(), idempotencyKey);
    }
}
