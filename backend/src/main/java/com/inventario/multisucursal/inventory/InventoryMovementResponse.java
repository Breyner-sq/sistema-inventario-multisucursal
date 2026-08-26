package com.inventario.multisucursal.inventory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * docs/API_DESIGN.md, sección 8: InventoryMovementDTO. {@code source} es
 * siempre {@code null} en esta fase — ningún movimiento de un ajuste manual
 * cuelga de un documento comercial (BR-023); los módulos `purchases`,
 * `sales` y `transfers` poblarán este campo cuando se implementen.
 */
public record InventoryMovementResponse(
        String id,
        String productId,
        String branchId,
        MovementDirection direction,
        MovementReason reason,
        BigDecimal quantity,
        String unitOfMeasureId,
        String responsibleUserId,
        Instant occurredAt,
        String notes,
        MovementSource source) {

    public static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(
                String.valueOf(movement.getId()),
                String.valueOf(movement.getProductId()),
                String.valueOf(movement.getBranchId()),
                movement.getDirection(),
                movement.getReason(),
                movement.getQuantity(),
                String.valueOf(movement.getUnitOfMeasureId()),
                String.valueOf(movement.getResponsibleUserId()),
                movement.getOccurredAt(),
                movement.getNotes(),
                null);
    }

    public record MovementSource(String type, String id) {
    }
}
