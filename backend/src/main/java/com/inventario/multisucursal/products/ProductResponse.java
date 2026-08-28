package com.inventario.multisucursal.products;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        String baseUnitOfMeasureId,
        boolean active,
        BigDecimal minimumStock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                String.valueOf(product.getId()),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                String.valueOf(product.getBaseUnitOfMeasureId()),
                product.isActive(),
                product.getMinimumStock());
    }
}
