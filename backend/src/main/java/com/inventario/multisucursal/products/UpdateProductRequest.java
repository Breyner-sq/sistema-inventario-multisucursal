package com.inventario.multisucursal.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * docs/API_DESIGN.md, sección 7.4: el PATCH actualiza "nombre/descripción" —
 * ni el SKU ni la unidad base son editables aquí (cambiar la unidad base de
 * un producto ya en uso es una operación estructural fuera de este alcance).
 */
public record UpdateProductRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description) {
}
