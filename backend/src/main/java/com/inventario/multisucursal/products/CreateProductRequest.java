package com.inventario.multisucursal.products;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * {@code minimumStock} (BR-010): el valor por defecto que recibirá el mínimo
 * de cada sucursal la primera vez que registre movimiento de este producto —
 * no una cantidad de stock del producto en sí (eso es de {@code Inventory},
 * por sucursal).
 */
public record CreateProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull Long baseUnitOfMeasureId,
        @NotNull @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo.") BigDecimal minimumStock) {
}
