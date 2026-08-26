package com.inventario.multisucursal.inventory;

/**
 * Catálogo completo de motivos (docs/DOMAIN_MODEL.md, sección 2.8). Esta
 * fase (ajuste manual) solo permite escribir con {@link #DEVOLUCION},
 * {@link #AJUSTE_INGRESO}, {@link #MERMA} o {@link #AJUSTE_RETIRO} —
 * {@link InventoryMovementService} lo valida explícitamente. Los demás
 * valores existen ya en el enum porque son parte del modelo de dominio
 * aprobado y del `CHECK` de la migración, pero ningún flujo de esta fase los
 * produce todavía: {@link #COMPRA} y {@link #VENTA}/{@link #TRANSFERENCIA_SALIDA}/
 * {@link #TRANSFERENCIA_ENTRADA} los generarán los módulos `purchases`,
 * `sales` y `transfers` cuando se implementen (condición de parada explícita
 * de esta fase).
 */
public enum MovementReason {
    COMPRA,
    DEVOLUCION,
    AJUSTE_INGRESO,
    VENTA,
    MERMA,
    AJUSTE_RETIRO,
    TRANSFERENCIA_SALIDA,
    TRANSFERENCIA_ENTRADA
}
