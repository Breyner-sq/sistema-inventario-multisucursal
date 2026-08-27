package com.inventario.multisucursal.sales;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        String id,
        String saleNumber,
        String branchId,
        String soldByUserId,
        SaleStatus status,
        Instant saleDate,
        List<SaleItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total) {

    public static SaleResponse from(Sale sale, List<SaleItem> items) {
        return new SaleResponse(
                String.valueOf(sale.getId()),
                sale.getSaleNumber(),
                String.valueOf(sale.getBranchId()),
                String.valueOf(sale.getSoldByUserId()),
                sale.getStatus(),
                sale.getSaleDate(),
                items.stream().map(SaleItemResponse::from).toList(),
                sale.getSubtotal(),
                sale.getDiscountTotal(),
                sale.getTotal());
    }
}
