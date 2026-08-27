package com.inventario.multisucursal.sales;

import java.math.BigDecimal;

/** docs/openapi.yaml, Sale.items[] — sin {@code id}: ninguna acción de cliente necesita referenciar una línea de venta después de creada. */
public record SaleItemResponse(
        String productId, BigDecimal quantity, String unitOfMeasureId, BigDecimal unitPrice, BigDecimal discountPercentage, BigDecimal lineTotal) {

    public static SaleItemResponse from(SaleItem item) {
        return new SaleItemResponse(
                String.valueOf(item.getProductId()),
                item.getQuantity(),
                String.valueOf(item.getUnitOfMeasureId()),
                item.getUnitPrice(),
                item.getDiscountPercentage(),
                item.getLineTotal());
    }
}
