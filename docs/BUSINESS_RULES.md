# Business Rules

## Reglas provenientes de la prueba técnica

### BR-001
Cada ingreso o retiro de inventario debe mantener
trazabilidad de fecha, responsable, motivo y cantidad.

### BR-002
Una venta debe validar disponibilidad de stock antes
de confirmarse.

### BR-003
Confirmar la recepción de una compra debe actualizar
el inventario.

### BR-004
Debe calcularse costo promedio ponderado.

### BR-005
La sucursal origen de una transferencia debe validar
disponibilidad antes del envío.

### BR-006
Una transferencia puede tener recepción completa.

### BR-007
Una transferencia puede tener recepción parcial.

### BR-008
Una recepción parcial debe registrar faltantes.

### BR-009
El faltante debe poder tratarse mediante reenvío,
ajuste o reclamación.

### BR-010
Debe existir control de stock mínimo.

### BR-011
Debe soportarse más de una unidad de medida.

## Decisiones todavía pendientes

Las invariantes técnicas completas se definirán
durante las fases de modelo de dominio,
reglas de negocio y flujos críticos.

No asumir todavía:

- estrategia de locking;
- idempotencia;
- máquina definitiva de estados;
- momento de reserva de stock;
- política de eliminación;
- política de redondeo;
- implementación de InventoryMovement.
