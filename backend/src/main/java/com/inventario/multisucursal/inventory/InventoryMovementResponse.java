package com.inventario.multisucursal.inventory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * docs/API_DESIGN.md, sección 8: InventoryMovementDTO. {@code source} es
 * {@code null} para un ajuste manual (BR-023, sin documento comercial);
 * para una recepción de compra (módulo `purchases`) queda poblado con
 * {@code type=PURCHASE_ORDER} y el id de la {@code PurchaseOrderItem} de
 * origen. Los módulos `sales`/`transfers` completarán los tipos restantes
 * cuando se implementen.
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
        MovementSource source = movement.getPurchaseOrderItemId() != null
                ? new MovementSource("PURCHASE_ORDER", String.valueOf(movement.getPurchaseOrderItemId()))
                : null;
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
                source);
    }

    public record MovementSource(String type, String id) {
    }
}
