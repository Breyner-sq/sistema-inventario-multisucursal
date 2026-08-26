package com.inventario.multisucursal.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * docs/API_DESIGN.md, sección 7.2: el PATCH actualiza "nombre/rol/sucursal" —
 * ni email ni contraseña (esos son flujos aparte, fuera de este alcance).
 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull RoleCode role,
        Long branchId) {
}
