# Reglas de Negocio

**Sistema de Inventario Multi-Sucursal**

**Base de este documento:** `docs/DOMAIN_MODEL.md` (modelo de dominio aprobado), `docs/PROJECT_BRIEF.md`, `docs/USE_CASES.md`, `docs/ARCHITECTURE.md` (secciones 6 y 7, ubicación de reglas y estrategia transaccional).

**Fecha:** 2026-08-26 (actualizado el mismo día tras el análisis de flujos críticos). Este documento reemplaza y expande la versión anterior de `BUSINESS_RULES.md` — las reglas BR-001 a BR-011 conservan el mismo identificador y significado que ya referencian otros documentos (`PROJECT_BRIEF.md`, `REQUIREMENTS_TRACEABILITY.md`, `DOMAIN_MODEL.md`); se agregan BR-012 en adelante para el resto de la cobertura pedida. Se agrega BR-023 y se corrigen BR-008 y BR-017 tras el análisis detallado en `docs/CRITICAL_FLOWS.md`, que contiene el pseudocódigo y los diagramas de actividad que hacen operativa cada regla — consúltese ese documento para el detalle paso a paso.

**Formato de cada regla:** ID, descripción, entidades afectadas, validación (qué se verifica y en qué capa), error esperado (código HTTP + slug de error) y pruebas necesarias. No se diseñan endpoints ni rutas concretas — el código de error es una convención de contrato, no una especificación de API.

No se escribe código en este documento.

---

## Convención de errores a nivel de API

Antes del catálogo, se fija la convención que cada regla usa en su campo "error esperado", para no repetirla 22 veces:

| Código HTTP | Cuándo aplica | Ejemplo |
|---|---|---|
| **400 Bad Request** | El payload es estructuralmente inválido: falta un campo obligatorio, tipo de dato incorrecto, formato inválido. Se detecta en la capa de entrada (DTO/Bean Validation), antes de llegar a cualquier regla de negocio. | Falta `productId` en el body de un movimiento. |
| **401 Unauthorized** | No hay JWT válido o está expirado. Transversal a todas las reglas; no se repite por regla porque no es una regla de negocio sino un requisito de acceso (RNF-003). | Token ausente o expirado. |
| **403 Forbidden** | El usuario está autenticado pero su rol o su alcance por sucursal no lo autoriza para la acción solicitada. | Un Operador intenta aprobar una transferencia. |
| **404 Not Found** | El recurso referenciado por identificador no existe. | `productId` inexistente. |
| **409 Conflict** | El **estado actual** del recurso impide la operación: transición de estado inválida, conflicto de versión por concurrencia, operación ya aplicada (idempotencia), documento ya cerrado. | Confirmar recepción de una orden de compra ya `RECEIVED`. |
| **422 Unprocessable Entity** | El payload tiene forma válida, pero viola una regla de negocio semántica sobre datos que sí existen y están en un estado válido. | Vender más unidades de las que hay en stock. |

Regla general para distinguir 409 de 422: **409 es "no en este momento/estado"**, **422 es "esto no es válido aunque el estado sea correcto"**. Ambos son errores de negocio (nunca del cliente HTTP en sí), por eso ninguno de los dos es 400.

---

## Catálogo de reglas

### BR-001 — Trazabilidad completa de movimientos de inventario **[Origen]**

- **Descripción:** todo ingreso o retiro de inventario debe registrarse con fecha, responsable, motivo y cantidad, sin excepción.
- **Entidades afectadas:** `InventoryMovement`, `Inventory`, `User`.
- **Validación:** la capa de aplicación de `inventory` no expone ninguna operación que modifique `Inventory.quantity_on_hand` sin crear, en la misma transacción, la fila de `InventoryMovement` correspondiente con `responsible_user_id`, `reason`, `occurred_at` y `quantity` poblados (`NOT NULL` a nivel de columna como respaldo).
- **Error esperado:** 400 si falta `motivo` o `cantidad` en la solicitud; no aplica un código de negocio porque esta regla se garantiza estructuralmente, no como una validación rechazable en tiempo de ejecución con datos completos.
- **Pruebas necesarias:** insertar un movimiento sin `responsible_user_id`/`reason`/`quantity` debe fallar a nivel de columna (`NOT NULL`); toda prueba de integración de RF-007/RF-008 debe verificar que exista exactamente un `InventoryMovement` nuevo por cada cambio de `Inventory.quantity_on_hand`.

### BR-002 — Validación de stock antes de confirmar una venta **[Origen]**

- **Descripción:** una venta no puede confirmarse si la cantidad solicitada de cualquiera de sus líneas excede el stock disponible de esa sucursal.
- **Entidades afectadas:** `Sale`, `SaleItem`, `Inventory`.
- **Validación:** en la capa de aplicación de `sales`, dentro de la misma transacción que confirma la venta, se compara `SaleItem.quantity` (convertida a unidad base) contra `Inventory.quantity_on_hand` de esa sucursal/producto, leído con bloqueo optimista (`version`).
- **Error esperado:** 422 `STOCK_INSUFICIENTE`.
- **Pruebas necesarias:** venta con cantidad exactamente igual al stock disponible se confirma y deja stock en cero; venta con cantidad mayor al stock disponible se rechaza sin generar ningún `InventoryMovement`; dos ventas concurrentes que en conjunto exceden el stock disponible — solo una se confirma (ver BR-022, concurrencia).

### BR-003 — Confirmar recepción de compra actualiza el inventario **[Origen]**

- **Descripción:** al confirmar la recepción (total o parcial) de una orden de compra, el inventario de la sucursal receptora se incrementa automáticamente en la cantidad recibida.
- **Entidades afectadas:** `PurchaseOrder`, `PurchaseOrderItem`, `InventoryMovement`, `Inventory`.
- **Validación:** el servicio de `purchases` invoca al servicio de `inventory` para generar un `InventoryMovement` (`direction = INGRESO`, `reason = COMPRA`) por la cantidad recibida, dentro de la misma transacción en que se actualiza `PurchaseOrderItem.quantity_received` y, si corresponde, `PurchaseOrder.status`.
- **Error esperado:** 422 `CANTIDAD_RECEPCION_EXCEDE_ORDENADO` si la cantidad a recibir excede `quantity_ordered - quantity_received` restante; 409 `ORDEN_YA_RECIBIDA` si la orden ya está en estado `RECEIVED` (ver BR-017, idempotencia).
- **Pruebas necesarias:** recepción total actualiza stock y cierra la orden; recepción parcial dos veces sucesivas (por el resto pendiente) actualiza el stock correctamente cada vez sin exceder lo ordenado; intentar recibir más de lo pendiente se rechaza.

### BR-004 — Cálculo de costo promedio ponderado **[Origen]**

- **Descripción:** cada recepción de compra con un precio unitario distinto al costo promedio actual del producto en esa sucursal recalcula el costo promedio ponderado.
- **Entidades afectadas:** `PurchaseOrderItem`, `Inventory`.
- **Fórmula:** `nuevo_costo = (stock_actual × costo_actual + cantidad_recibida × precio_unitario_recibido) / (stock_actual + cantidad_recibida)`.
- **Ajuste al modelo de dominio (aprobado y aplicado):** `docs/DOMAIN_MODEL.md` no incluía originalmente una columna para almacenar el costo promedio vigente; se aprobó agregar `Inventory.average_unit_cost` (numérico, `CHECK (average_unit_cost >= 0)`), actualizado exclusivamente por el flujo de recepción de compra (ver `docs/DOMAIN_MODEL.md`, sección 2.7 y 3.2).
- **Validación:** el recálculo ocurre en la misma transacción que BR-003, usando el `Inventory.quantity_on_hand` **previo** a aplicar el ingreso (no el posterior) como base de la ponderación.
- **Error esperado:** no es una regla rechazable por el usuario (no hay un caso de "costo promedio inválido" que el cliente pueda provocar con datos correctos); un resultado con precisión decimal se redondea según la política de redondeo — **pendiente de definir** (ver `docs/DOMAIN_MODEL.md`, decisiones pendientes de `docs/STATUS.md`).
- **Pruebas necesarias:** caso numérico documentado (100 u. a $10, ingreso de 50 u. a $16 → costo $12, ver `USE_CASES.md` HU-01); caso con stock inicial en cero (el costo promedio pasa a ser directamente el precio de la primera recepción); caso con dos recepciones sucesivas a precios distintos.

### BR-005 — Validación de disponibilidad antes de aprobar/preparar el envío de una transferencia **[Origen]**

- **Descripción:** la sucursal origen debe validar que tiene disponibilidad suficiente antes de aprobar o confirmar la cantidad a enviar de una solicitud de transferencia.
- **Entidades afectadas:** `Transfer`, `TransferItem`, `Inventory`.
- **Validación:** al pasar de `REQUESTED` a `APPROVED`, el servicio de `transfers` compara `TransferItem.quantity_requested` (o la cantidad ajustada) contra `Inventory.quantity_on_hand` de la sucursal origen.
- **Error esperado:** 422 `STOCK_INSUFICIENTE_PARA_TRANSFERENCIA` si se intenta aprobar una cantidad mayor a la disponible; no impide rechazar la solicitud (`REJECTED`) ni aprobarla con una cantidad ajustada menor.
- **Pruebas necesarias:** aprobar con cantidad ajustada igual al stock disponible; intentar aprobar la cantidad íntegra solicitada cuando excede el stock disponible se rechaza, mostrando el ajuste posible.

### BR-006 — Recepción completa actualiza el inventario destino **[Origen]**

- **Descripción:** al confirmar que la cantidad recibida coincide con la despachada, el inventario de la sucursal destino se incrementa automáticamente y la transferencia pasa a `RECEIVED_COMPLETE`.
- **Entidades afectadas:** `Transfer`, `TransferItem`, `InventoryMovement`, `Inventory`.
- **Validación:** el servicio de `transfers` genera un `InventoryMovement` (`direction = INGRESO`, `reason = TRANSFERENCIA_ENTRADA`) por `quantity_shipped` completo, y cambia `Transfer.status`, dentro de una única transacción.
- **Error esperado:** 409 `TRANSFERENCIA_ESTADO_INVALIDO` si la transferencia no está en `IN_TRANSIT`.
- **Pruebas necesarias:** recepción completa incrementa el stock destino exactamente en `quantity_shipped`; intentar confirmar recepción sobre una transferencia en estado `REQUESTED` o `RECEIVED_COMPLETE` se rechaza.

### BR-007 — Soporte de recepción parcial **[Origen]**

- **Descripción:** el sistema debe permitir confirmar una recepción con cantidad menor a la despachada, sin bloquear el flujo ni forzar una recepción completa ficticia.
- **Entidades afectadas:** `Transfer`, `TransferItem`.
- **Validación:** el servicio de `transfers` acepta `quantity_received < quantity_shipped` como un caso válido, no como un error, y deriva el estado `RECEIVED_PARTIAL` (ver BR-020, máquina de estados).
- **Error esperado:** no aplica error — es el camino feliz de un caso legítimo, distinto de BR-014 (recepción que excede lo enviado, que sí es un error).
- **Pruebas necesarias:** recepción con cantidad menor a la despachada se acepta y deriva a `RECEIVED_PARTIAL`, no a un error.

### BR-008 — La recepción parcial debe registrar el faltante **[Origen]**

- **Descripción:** toda recepción parcial calcula y persiste la diferencia entre lo despachado y lo recibido.
- **Entidades afectadas:** `TransferItem`.
- **Validación:** al confirmar la recepción, el servicio calcula `quantity_missing = quantity_shipped - quantity_received` y lo persiste junto con `quantity_received` en la misma operación; también dispara una notificación de discrepancia abierta (RF-026). **Corrección respecto a la versión anterior de esta regla:** esta notificación **no** reutiliza `StockAlert` (esa entidad es específica para stock mínimo, BR-010) — es un evento distinto cuya condición de "abierta" se consulta directamente como `TransferItem.quantity_missing > 0 AND discrepancy_treatment IS NULL`, sin necesitar una tabla propia (ver `docs/CRITICAL_FLOWS.md`, flujo F1).
- **Error esperado:** no aplica error propio; ver BR-014 para el caso en que `quantity_received > quantity_shipped`.
- **Pruebas necesarias:** recepción de 45 de 50 unidades registra `quantity_missing = 5`; recepción completa dentro del mismo flujo deja `quantity_missing` en `NULL` o `0`, no ambiguo.

### BR-009 — Tratamiento del faltante: reenvío, ajuste o reclamación **[Origen]**

- **Descripción:** todo faltante registrado (BR-008) debe poder resolverse mediante uno de tres tratamientos: reenvío, ajuste o reclamación.
- **Entidades afectadas:** `TransferItem`, `Transfer` (si el tratamiento es reenvío, se crea una nueva `Transfer` referenciada en `follow_up_transfer_id`).
- **Validación:** el servicio de `transfers` solo acepta el tratamiento sobre un `TransferItem` en estado `RECEIVED_PARTIAL` con `quantity_missing > 0` y sin tratamiento previo (`discrepancy_treatment IS NULL`); un tratamiento ya definido no se puede sobrescribir (ver BR-021, no se elimina/edita historial).
- **Error esperado:** 409 `FALTANTE_YA_TRATADO` si se intenta definir un tratamiento sobre una línea que ya lo tiene; 422 `TRATAMIENTO_INVALIDO` si el valor no es uno de los tres permitidos.
- **Pruebas necesarias:** definir tratamiento `REENVIO` crea una nueva `Transfer` vinculada; intentar tratar dos veces la misma línea se rechaza; intentar tratar una línea sin faltante (`quantity_missing` nulo) se rechaza.

### BR-010 — Control de stock mínimo y generación de alerta **[Origen]**

- **Descripción:** cuando el stock de un producto en una sucursal alcanza o cae por debajo de su `minimum_stock`, se genera una alerta; cuando vuelve a superarlo, la alerta activa se resuelve.
- **Entidades afectadas:** `Inventory`, `StockAlert`.
- **Validación:** cada operación que modifica `Inventory.quantity_on_hand` (venta, retiro, ajuste, recepción de transferencia) evalúa, en la misma transacción, si el nuevo valor cruza el umbral `minimum_stock` en cualquier dirección, y crea o resuelve la `StockAlert` correspondiente. El índice único parcial `UNIQUE (inventory_id) WHERE status = 'ACTIVE'` (`docs/DOMAIN_MODEL.md`, 2.9) evita alertas activas duplicadas incluso ante una condición de carrera.
- **Error esperado:** no aplica error — es un efecto colateral automático, no una operación que el usuario invoque directamente y pueda rechazarse.
- **Pruebas necesarias:** un retiro que deja el stock justo en el mínimo genera la alerta; un ingreso posterior que sube el stock por encima del mínimo resuelve la alerta activa; dos retiros concurrentes que cruzan el umbral casi simultáneamente generan una única alerta activa, no dos.

### BR-011 — Soporte de múltiples unidades de medida con conversión **[Origen]**

- **Descripción:** un producto puede registrarse, comprarse o venderse en una unidad de medida distinta a su unidad base, siempre que exista una conversión definida.
- **Entidades afectadas:** `Product`, `ProductUnit`, `UnitOfMeasure`, `InventoryMovement`, `PurchaseOrderItem`, `SaleItem`, `TransferItem`.
- **Validación:** toda operación que registre una cantidad en una unidad no base primero verifica que exista un `ProductUnit` para ese `(product_id, unit_of_measure_id)`, y convierte la cantidad a la unidad base usando `conversion_factor_to_base` antes de aplicarla a `Inventory.quantity_on_hand`.
- **Error esperado:** 422 `UNIDAD_NO_SOPORTADA` si se intenta registrar una cantidad en una unidad sin conversión definida para ese producto.
- **Pruebas necesarias:** registrar un ingreso en una unidad no base convierte correctamente antes de sumar al agregado; intentar usar una unidad no definida para el producto se rechaza.

### BR-012 — Cantidades siempre positivas en operaciones de entrada/salida

- **Descripción:** ninguna cantidad registrada en un movimiento de inventario, línea de compra, línea de venta o línea de transferencia puede ser cero o negativa.
- **Entidades afectadas:** `InventoryMovement.quantity`, `PurchaseOrderItem.quantity_ordered`, `SaleItem.quantity`, `TransferItem.quantity_requested/quantity_approved/quantity_shipped/quantity_received`.
- **Validación:** verificación en la capa de entrada (forma: es numérico y mayor que cero) y reforzada con `CHECK (quantity > 0)` en cada tabla correspondiente (`docs/DOMAIN_MODEL.md`, sección 6).
- **Error esperado:** 400 si el campo no es numérico o falta; 422 `CANTIDAD_INVALIDA` si es numérico pero ≤ 0 (es una regla de negocio, no un error de forma, porque "cero" es un valor sintácticamente válido).
- **Pruebas necesarias:** cantidad `0` y cantidad negativa se rechazan en cada uno de los cuatro puntos de entrada listados; cantidad `null` se rechaza como 400, no como 422.

### BR-013 — Una transferencia no puede despachar más de lo aprobado ni de lo disponible al momento del despacho

- **Descripción:** al registrar el despacho (RF-024), la cantidad a enviar no puede exceder `quantity_approved`, y tampoco puede exceder el stock realmente disponible en ese momento (que pudo cambiar desde la aprobación por otra venta o movimiento).
- **Entidades afectadas:** `TransferItem`, `Inventory`.
- **Validación:** el servicio de `transfers`, dentro de la transacción de despacho, valida `quantity_shipped <= quantity_approved` y, adicionalmente, vuelve a comprobar `Inventory.quantity_on_hand >= quantity_shipped` con lectura bloqueada por `version` — no confía en la validación hecha en el momento de la aprobación (BR-005), porque el tiempo transcurrido entre aprobar y despachar puede haber consumido el stock reservado (ver `docs/USE_CASES.md`, UC-09, flujo alterno 1a).
- **Error esperado:** 422 `CANTIDAD_DESPACHO_EXCEDE_APROBADO` si excede lo aprobado; 422 `STOCK_INSUFICIENTE` si excede el stock real disponible al despachar.
- **Pruebas necesarias:** despacho con cantidad igual a la aprobada y con stock suficiente se acepta; despacho con cantidad mayor a la aprobada se rechaza; escenario donde una venta consume el stock entre la aprobación y el despacho hace que el despacho se rechace pese a estar aprobado.

### BR-014 — La recepción no puede superar lo efectivamente enviado, salvo corrección explícita

- **Descripción:** `TransferItem.quantity_received` nunca puede ser mayor que `TransferItem.quantity_shipped` mediante el flujo normal de recepción.
- **Entidades afectadas:** `TransferItem`.
- **Validación:** verificación en el servicio de `transfers` antes de persistir la recepción, reforzada con `CHECK (quantity_received IS NULL OR quantity_received <= quantity_shipped)` en base de datos. No existe, en el alcance actual, un "flujo explícito de corrección" que permita recibir de más — si se necesitara (p. ej. el destino cuenta físicamente más de lo que el origen declaró haber despachado), quedaría como un ajuste manual de inventario en la sucursal destino, registrado aparte con su propio motivo (`AJUSTE_INGRESO`), nunca sobrescribiendo `quantity_received` de la transferencia.
- **Error esperado:** 422 `RECEPCION_EXCEDE_ENVIADO`.
- **Pruebas necesarias:** recepción con cantidad mayor a la despachada se rechaza; verificar que la corrección vía ajuste manual (fuera de esta regla) no modifica los campos de la transferencia original.

### BR-015 — Consistencia entre el stock materializado y el ledger de movimientos

- **Descripción:** `Inventory.quantity_on_hand` debe ser, en todo momento, igual a la suma de ingresos menos retiros registrados en `InventoryMovement` para ese producto/sucursal.
- **Entidades afectadas:** `Inventory`, `InventoryMovement`.
- **Validación:** estructural, no una validación que el usuario pueda disparar — se garantiza porque el único camino de escritura a `Inventory.quantity_on_hand` es el servicio de `inventory`, que siempre inserta el `InventoryMovement` correspondiente en la misma transacción (ver ADR-008 y `docs/DOMAIN_MODEL.md`, decisión 3.2). Se recomienda una comprobación periódica (job de auditoría, fuera del alcance de esta prueba) que recalcule la suma de movimientos y la compare contra el agregado, para detectar una eventual divergencia por error de programación.
- **Error esperado:** no aplica un error de API — una divergencia sería un defecto interno, no una respuesta a una solicitud del cliente.
- **Pruebas necesarias:** prueba de integración que, tras una secuencia de ingresos y retiros mixtos (compra, venta, transferencia, ajuste), recalcula la suma de `InventoryMovement` y verifica que coincide exactamente con `Inventory.quantity_on_hand`.

### BR-016 — La recepción de compra actualiza inventario y costo promedio de forma atómica

- **Descripción:** la actualización de `Inventory.quantity_on_hand`, la creación del `InventoryMovement` y el recálculo de `Inventory.average_unit_cost` (BR-004) ocurren como una única unidad atómica; no puede persistirse el cambio de stock sin el recálculo de costo, ni viceversa.
- **Entidades afectadas:** `PurchaseOrderItem`, `InventoryMovement`, `Inventory`.
- **Validación:** todo el bloque se ejecuta dentro de un único `@Transactional` en el servicio de `purchases`/`inventory` (`docs/ARCHITECTURE.md`, sección 7); si cualquier paso falla (p. ej. el `CHECK (quantity_on_hand >= 0)` improbable aquí, o una violación de unicidad), toda la transacción revierte — no queda un stock actualizado con un costo promedio desactualizado, ni viceversa.
- **Error esperado:** 500 si ocurre un fallo interno a mitad de la operación (no es un error de negocio del cliente, es una garantía de atomicidad); ningún estado parcial debe ser observable externamente.
- **Pruebas necesarias:** simular un fallo forzado entre la actualización de stock y el recálculo de costo (prueba de integración con un punto de fallo inyectado) y verificar que ninguno de los dos cambios queda aplicado tras el rollback.

### BR-017 — Idempotencia de operaciones críticas ante reintento accidental

- **Descripción:** un reintento accidental (doble clic, reintento de red, doble entrega de un mensaje) de una operación crítica no debe aplicar el efecto dos veces.
- **Entidades afectadas:** `PurchaseOrder`/`PurchaseOrderItem`, `Sale`, `InventoryMovement`, `Transfer`/`TransferItem`.
- **Corrección respecto a la versión anterior de esta regla:** el análisis detallado de `docs/CRITICAL_FLOWS.md` (sección 1.1) mostró que **no todas** las operaciones críticas se protegen solo con el estado del recurso — depende de si la operación es una transición de un solo uso o una creación/evento repetible:
  - **Transiciones de un solo uso (guarda de estado, sin clave de idempotencia):** aprobar/rechazar transferencia, despachar, confirmar recepción completa/parcial, cerrar tras tratamiento, confirmar una venta. Se protegen con `UPDATE ... WHERE status = <esperado>`; si la fila ya cambió, `0` filas afectadas → 409.
  - **Creación o evento repetible (requiere `idempotency_key` provista por el cliente):** registrar una venta (`Sale.client_reference_id`), confirmar una recepción de compra (`InventoryMovement.idempotency_key` — una orden `PARTIALLY_RECEIVED` admite legítimamente una siguiente recepción, por lo que el estado por sí solo no distingue un reintento de la siguiente operación real) y un ajuste manual de inventario. Ver columnas pendientes de aprobación en la sección final de este documento.
  - En ambos casos, la comprobación y la escritura ocurren en la misma transacción, con el mismo bloqueo (`version`) usado para concurrencia (BR-022), de modo que dos solicitudes casi simultáneas no pasen ambas la comprobación antes de que la primera confirme su cambio.
- **Error esperado:** 409 `OPERACION_YA_APLICADA` para las transiciones de un solo uso (o el slug específico de estado, p. ej. `ORDEN_YA_RECIBIDA`, `TRANSFERENCIA_ESTADO_INVALIDO`); para las operaciones de categoría "creación repetible", un reintento con la misma `idempotency_key` **no es un error** — se responde con el mismo resultado ya creado.
- **Pruebas necesarias:** enviar la misma confirmación dos veces en sucesión rápida (simulando un doble clic) debe aplicar el efecto una sola vez; para las operaciones de creación repetible, reenviar la misma `idempotency_key` debe devolver el resultado original sin duplicar el efecto, y una `idempotency_key` distinta en una recepción parcial legítima subsiguiente sí debe aplicarse. Ver `docs/CRITICAL_FLOWS.md`, escenario 3.3, para el detalle completo por operación.

### BR-018 — Autorización por rol y por alcance de sucursal

- **Descripción:** toda acción de escritura está limitada por el rol del usuario autenticado y, cuando corresponde, por su sucursal asignada (`docs/USE_CASES.md`, matriz Actor×Acción).
- **Entidades afectadas:** transversal — `User.role_code`, `User.branch_id` frente al `branch_id` del recurso sobre el que se opera.
- **Validación:** dos comprobaciones independientes, ambas necesarias: (1) el rol del usuario tiene permitida la acción (RBAC declarativo, `docs/adr/ADR-005-jwt-rbac.md`); (2) si la acción es de escritura sobre un recurso con `branch_id` (venta, movimiento, orden de compra, línea de transferencia como origen), el `branch_id` del usuario coincide con el del recurso — excepto `ADMIN`, con alcance global. La lectura de inventario/catálogo de otra sucursal está permitida a todos los roles (RF-003) y no aplica esta segunda comprobación.
- **Error esperado:** 403 `ROL_NO_AUTORIZADO` si el rol no tiene el permiso; 403 `SUCURSAL_NO_AUTORIZADA` si el rol lo permite en general pero el usuario no pertenece a la sucursal del recurso.
- **Pruebas necesarias:** un Operador de la sucursal A no puede registrar una venta en la sucursal B; un Gerente no puede gestionar usuarios/sucursales; un Administrador puede operar sobre cualquier sucursal.

### BR-019 — Las listas de precios y descuentos no producen totales inválidos

- **Descripción:** el precio unitario aplicado y el descuento de una línea de venta o de compra no pueden producir un total negativo ni un descuento fuera de un rango válido.
- **Entidades afectadas:** `SaleItem`, `PurchaseOrderItem`, `Price`.
- **Validación:** `unit_price > 0`; `discount_percentage` entre `0` y `100` inclusive (o el equivalente si se modela como monto: `discount_amount >= 0` y `discount_amount <= unit_price × quantity`); `line_total = (unit_price × quantity) - descuento`, y se verifica `line_total >= 0` antes de persistir. El precio usado en una venta debe provenir de un `Price` vigente (`valid_to IS NULL`) de una `PriceList` activa — no se acepta un precio arbitrario tecleado libremente fuera de las listas configuradas, salvo que el diseño de UI decida lo contrario en una fase posterior (fuera del alcance de esta regla).
- **Error esperado:** 422 `DESCUENTO_FUERA_DE_RANGO`; 422 `TOTAL_INVALIDO` si el cálculo resulta negativo.
- **Pruebas necesarias:** descuento del 100% dejando el total en cero se acepta (es el límite válido); descuento mayor al 100% o un monto de descuento mayor al subtotal de la línea se rechaza; venta usando un precio de una lista inactiva o de una versión de precio ya cerrada (`valid_to` no nulo) se rechaza.

### BR-020 — Los cambios de estado de una transferencia siguen la máquina de estados válida

- **Descripción:** `Transfer.status` solo puede transicionar según el diagrama definido en `docs/DOMAIN_MODEL.md`, sección 4: `REQUESTED → APPROVED|REJECTED`, `APPROVED → IN_TRANSIT`, `IN_TRANSIT → RECEIVED_COMPLETE|RECEIVED_PARTIAL`, `RECEIVED_PARTIAL → CLOSED`.
- **Entidades afectadas:** `Transfer`.
- **Validación:** cada operación que cambia `status` (aprobar, rechazar, despachar, recibir, cerrar) verifica explícitamente el estado actual antes de aplicar la transición; ninguna transición fuera de esta lista es aceptada, incluida cualquier intento de "retroceder" un estado.
- **Error esperado:** 409 `TRANSICION_INVALIDA`.
- **Pruebas necesarias:** cada transición válida se prueba individualmente; se prueban explícitamente transiciones inválidas representativas (p. ej. `REQUESTED → IN_TRANSIT` directo, `RECEIVED_COMPLETE → APPROVED`, `CLOSED → cualquier estado`) y todas deben rechazarse con 409.

### BR-021 — No se elimina historial auditable

- **Descripción:** ningún registro de `InventoryMovement`, `Sale`/`SaleItem`, `PurchaseOrder`/`PurchaseOrderItem`, `Transfer`/`TransferItem` ni versión de `Price` puede eliminarse ni editarse retroactivamente en sus campos históricos, una vez creado (`docs/DOMAIN_MODEL.md`, sección 3.9).
- **Entidades afectadas:** todas las listadas arriba, más `Branch`, `Product`, `Supplier`, `User` (baja lógica, nunca eliminación física si tienen historial).
- **Validación:** no se expone ninguna operación de `DELETE` para estas entidades en la capa de aplicación; a nivel de base de datos, se recomienda revocar los privilegios `DELETE`/`UPDATE` sobre las tablas de historial puro (`InventoryMovement`) al rol de aplicación, y usar `ON DELETE RESTRICT` en las claves foráneas hacia `Branch`/`Product`/`Supplier`/`User` para que ni siquiera un error de programación pueda eliminarlos si tienen historial asociado.
- **Error esperado:** 404 si se intenta acceder a una operación de eliminación inexistente en la API (no se expone la ruta); 409 `ENTIDAD_CON_HISTORIAL` si se intenta desactivar/eliminar una `Branch`/`Product`/`Supplier`/`User` que tiene historial y el diseño decide validarlo explícitamente en vez de dejar que la sola falta de un endpoint de borrado lo prevenga.
- **Pruebas necesarias:** intentar eliminar físicamente un producto con movimientos asociados falla por restricción de clave foránea; ninguna prueba de la suite debe requerir editar un `InventoryMovement`, `SaleItem` o `Price` ya creado.

### BR-022 — Concurrencia sobre el mismo stock: bloqueo optimista y reintento

- **Descripción:** dos operaciones concurrentes que afectan el mismo `Inventory` (misma combinación producto/sucursal) no deben poder aplicar ambas su efecto sobre una lectura obsoleta del stock.
- **Entidades afectadas:** `Inventory` (columna `version`).
- **Validación:** toda escritura sobre `Inventory.quantity_on_hand` (y, por el mismo motivo, sobre `PurchaseOrderItem.quantity_received`, ver ajustes pendientes) lee la fila junto con su `version` actual y, al escribir, verifica que la versión no haya cambiado (bloqueo optimista, `docs/ARCHITECTURE.md`, sección 7); si cambió, la transacción falla con conflicto de versión y el servicio reintenta automáticamente la operación completa (releyendo el stock actualizado). **Mecanismo definido en `docs/CRITICAL_FLOWS.md` (sección 1.2):** máximo 3 intentos, con un backoff aleatorio corto entre reintentos, antes de propagar el error al cliente.
- **Error esperado:** si se agotan los reintentos automáticos, 409 `CONFLICTO_CONCURRENCIA`. El cliente no debería ver esto en el uso normal — es la salida de emergencia cuando la contención es tan alta que ni el reintento automático la resuelve.
- **Pruebas necesarias:** prueba de concurrencia con N hilos escribiendo sobre el mismo `Inventory` simultáneamente verifica que el resultado final es exactamente la suma/resta esperada, sin pérdidas de actualización ("lost update"); prueba que fuerza el agotamiento de reintentos y verifica el código 409.

### BR-023 — El ajuste manual de inventario es una operación de excepción, no un sustituto de compra/venta/transferencia

- **Descripción:** un ajuste manual (`AJUSTE_INGRESO`/`AJUSTE_RETIRO`) existe únicamente para corregir discrepancias entre el stock del sistema y el conteo físico real (mermas no capturadas, error de captura, hallazgos de inventario físico); nunca debe usarse como sustituto de un flujo de negocio ya modelado.
- **Entidades afectadas:** `InventoryMovement`, `Inventory`.
- **Validación:** `notes` (motivo) es obligatorio y no puede quedar vacío — a diferencia de otros movimientos, aquí no basta el valor del `ENUM reason`, porque un ajuste necesita una justificación legible por auditoría; el movimiento no puede tener poblada ninguna FK documental (`purchase_order_item_id`, `sale_item_id`, `transfer_item_id`), ya que un ajuste no cuelga de un documento comercial; se aplican además BR-012 (cantidad positiva) y la prevención general de stock negativo.
- **Error esperado:** 400 `NOTES_REQUERIDO` si falta el motivo; 422 `CANTIDAD_INVALIDA`; 422 `STOCK_INSUFICIENTE` si el ajuste de retiro dejaría el stock en negativo.
- **Pruebas necesarias:** ajuste sin `notes` se rechaza; ajuste de retiro mayor al stock disponible se rechaza igual que cualquier otro retiro; ver `docs/CRITICAL_FLOWS.md`, flujo G, para el detalle de idempotencia (requiere `idempotency_key`, es una operación de creación repetible).

### BR-024 — Consistencia rol/sucursal al crear o editar un usuario **[Origen: RF-037 a RF-039; DOMAIN_MODEL.md, sección 2.3]**

- **Descripción:** un usuario `ADMIN` no debe tener sucursal asignada (alcance global); un usuario `MANAGER`/`OPERATOR` siempre debe tener una sucursal asignada, que además debe existir y estar activa. Regla añadida al implementar UC-14 — no estaba enumerada explícitamente en versiones anteriores de este catálogo, aunque ya se derivaba del `CHECK` de `docs/DOMAIN_MODEL.md`.
- **Entidades afectadas:** `User`, `Branch`.
- **Validación:** `users.UserService` valida esto en la capa de aplicación antes de escribir (defensa en profundidad), replicando el `CHECK (role_code = 'ADMIN' OR branch_id IS NOT NULL)` de `V3__create_users_table.sql`, que actúa como última línea de defensa en base de datos.
- **Error esperado:** 422 `ADMIN_SIN_SUCURSAL` si `role=ADMIN` y se envía `branchId`; 422 `SUCURSAL_REQUERIDA` si `role≠ADMIN` y no se envía `branchId`; 404 `SUCURSAL_NO_ENCONTRADA` si el `branchId` no existe; 422 `SUCURSAL_INACTIVA` si la sucursal existe pero está desactivada.
- **Pruebas necesarias:** las cuatro combinaciones inválidas anteriores se rechazan con su código específico; crear/editar un `MANAGER`/`OPERATOR` con una sucursal activa existente se acepta; crear un `ADMIN` sin sucursal se acepta.

### BR-025 — No desactivar una sucursal con usuarios activos asignados **[Origen: UC-15, flujo alterno 1a]**

- **Descripción:** no se puede desactivar una sucursal mientras tenga usuarios activos (`MANAGER`/`OPERATOR`) asignados — dejaría esos usuarios apuntando a una sucursal inactiva, una inconsistencia análoga a la que UC-15 pide evitar para inventario/transferencias (módulos todavía no implementados; esta es la instancia concreta de la regla que sí se puede aplicar hoy).
- **Entidades afectadas:** `Branch`, `User`.
- **Validación:** `branches.BranchService.deactivate` comprueba `existsByBranchIdAndActiveTrue` antes de desactivar. No impide desactivar una sucursal sin usuarios activos (ya reasignados o desactivados primero).
- **Error esperado:** 409 `SUCURSAL_CON_USUARIOS_ACTIVOS`.
- **Pruebas necesarias:** desactivar una sucursal con al menos un usuario activo asignado se rechaza; desactivarla después de reasignar/desactivar a todos sus usuarios se acepta.

### BR-026 — La unidad base de un producto es inmutable **[Decisión]**

- **Descripción:** el factor de conversión de la unidad base de un producto (siempre `1`, por definición) no se puede modificar mediante el endpoint de edición de unidades alternativas. Regla añadida al implementar el módulo `products` para satisfacer el requisito de conversiones deterministas de BR-011 — no estaba enumerada explícitamente en versiones anteriores de este catálogo.
- **Entidades afectadas:** `ProductUnit`.
- **Validación:** `products.ProductUnitService.updateConversionFactor` rechaza la operación si el `ProductUnit` objetivo tiene `is_base_unit = true`, antes de aplicar el cambio.
- **Error esperado:** 422 `UNIDAD_BASE_INMUTABLE`.
- **Pruebas necesarias:** intentar editar el factor de conversión de la unidad base de un producto se rechaza con este código; editar el factor de una unidad alternativa (no base) se acepta.

### BR-027 — El motivo de un ajuste manual debe ser compatible con su dirección **[Decisión]**

- **Descripción:** un ajuste manual (BR-023) con `direction=INGRESO` solo admite `reason=DEVOLUCION` o `AJUSTE_INGRESO`; con `direction=RETIRO` solo admite `MERMA` o `AJUSTE_RETIRO`. `COMPRA`, `VENTA`, `TRANSFERENCIA_SALIDA`/`TRANSFERENCIA_ENTRADA` existen en el catálogo de `docs/DOMAIN_MODEL.md` (sección 2.8) pero ningún flujo de esta fase los produce — quedan reservados para cuando se implementen `purchases`/`sales`/`transfers`. Si no se envía `reason`, se asume `AJUSTE_INGRESO`/`AJUSTE_RETIRO` según la dirección. Regla añadida al implementar el módulo `inventory` — no estaba enumerada explícitamente en versiones anteriores de este catálogo.
- **Entidades afectadas:** `InventoryMovement`.
- **Validación:** `inventory.InventoryMovementService.resolveReason` verifica la pertenencia del motivo recibido al conjunto permitido para la dirección declarada, antes de crear el movimiento.
- **Error esperado:** 422 `MOTIVO_INCOMPATIBLE_CON_DIRECCION`.
- **Pruebas necesarias:** un ajuste `INGRESO` con `reason=MERMA` (u otro motivo de salida) se rechaza; un ajuste `INGRESO` con `reason=DEVOLUCION` se acepta; sin `reason`, el motivo por defecto corresponde a la dirección enviada.

### BR-028 — El precio de recepción es distinto del precio pactado en la orden **[Decisión]**

- **Descripción:** `PurchaseOrderItem.unit_price` (fijado al crear la orden) es la condición comercial pactada — inmutable, se usa para el `line_total` de la orden. El `unitPrice` que se envía en `POST /purchase-orders/{id}/receipts` (docs/openapi.yaml, `PurchaseReceiptRequest`) es el costo efectivamente recibido en esa recepción — puede diferir del pactado (cambios de precio entre orden y entrega, ajuste de factura) — y se usa exclusivamente para recalcular `Inventory.average_unit_cost` (BR-004). Ninguno sobrescribe al otro. Decisión tomada al implementar el módulo `purchases` para reconciliar una aparente tensión entre `docs/DOMAIN_MODEL.md` (sección 2.7, que sugiere que el costo histórico vive únicamente en `PurchaseOrderItem.unit_price`) y la firma explícita de `recibirCompra(..., precioUnitario, ...)` en `docs/CRITICAL_FLOWS.md` (flujo B) más el campo `unitPrice` del propio `PurchaseReceiptRequest` — se prioriza el pseudocódigo y el contrato ya aprobados, por ser más detallados y posteriores.
- **Entidades afectadas:** `PurchaseOrderItem`, `Inventory`.
- **Validación:** `PurchaseReceiptService.applyInventoryReceipt` usa el `unitPrice` de la solicitud de recepción, nunca el de `PurchaseOrderItem`, para la fórmula de costo promedio ponderado.
- **Error esperado:** no aplica — es una decisión de diseño, no una regla rechazable.
- **Pruebas necesarias:** recibir a un precio distinto del pactado en la orden actualiza `average_unit_cost` según el precio de recepción, sin alterar `PurchaseOrderItem.unit_price` ni su `lineTotal` original.

### BR-029 — Clave de idempotencia derivada por línea en la recepción de compra **[Decisión]**

- **Descripción:** `POST /purchase-orders/{id}/receipts` recibe un único encabezado `Idempotency-Key` por solicitud, pero puede traer varias líneas (`docs/openapi.yaml`, `PurchaseReceiptRequest.items[]`) y `InventoryMovement.idempotency_key` es única por movimiento (uno por línea). Se deriva una clave por línea como `<Idempotency-Key>:<purchaseOrderItemId>`. Esto permite reintentar la solicitud completa (cada línea ya aplicada se detecta y no se reaplica, devolviendo el resultado original) sin que el mismo header choque con otra recepción legítima que use una línea distinta de la misma orden.
- **Entidades afectadas:** `InventoryMovement`.
- **Validación:** `PurchaseReceiptService.receive` calcula la clave derivada por cada línea antes de comprobar `InventoryMovementRepository.findByIdempotencyKey`; esta comprobación ocurre **antes** que la de `PurchaseOrder.status` (BR-017/BR-023 generalizado a este flujo) — un reintento legítimo debe replicar su resultado incluso si la propia recepción original ya dejó la orden en `RECEIVED`.
- **Error esperado:** no aplica un error propio — un reintento con la misma clave no es un error (BR-017).
- **Pruebas necesarias:** reenviar la misma solicitud completa (mismo `Idempotency-Key`, mismas líneas) no duplica el incremento de stock ni el `quantity_received`; reenviar la misma clave después de que la orden ya quedó `RECEIVED` por ese mismo envío devuelve 200 con el resultado original, no 409; una clave distinta sí aplica una recepción legítima subsiguiente.

### BR-030 — Resolución de la lista de precios cuando la venta no especifica una **[Decisión]**

- **Descripción:** `POST /sales` acepta `priceListId` opcional (docs/openapi.yaml, `SaleCreateRequest`). Si se omite, se resuelve: primero una `PriceList` activa propia de la sucursal (`branch_id` = sucursal de la venta); si no existe, la lista global activa (`branch_id IS NULL`). Ningún documento aprobado detalla este algoritmo de resolución — es la interpretación más directa de "`branch_id` nulo = lista global" (docs/DOMAIN_MODEL.md, sección 2.13), priorizando lo específico de la sucursal sobre lo global.
- **Entidades afectadas:** `PriceList`, `Sale`.
- **Validación:** `SaleService.resolvePriceList` — si se especifica `priceListId`, se usa directamente (validando que exista y esté activa); si no, aplica el orden de prioridad descrito.
- **Error esperado:** 404 `LISTA_PRECIOS_NO_ENCONTRADA` si se especifica un id inexistente; 409 `LISTA_PRECIOS_INACTIVA` si existe pero está inactiva; 422 `LISTA_PRECIOS_NO_ENCONTRADA` si no se especifica ninguna y no hay lista activa aplicable (ni de sucursal ni global) — 422 en vez de 404 porque no falta un recurso referenciado por id, sino que el estado de las listas de precios existentes no permite completar la operación.
- **Pruebas necesarias:** venta sin `priceListId` con una lista de sucursal activa usa esa lista; sin lista de sucursal, cae a la global; sin ninguna lista activa, se rechaza.

### BR-031 — El estado `VOIDED` de una venta no se implementa todavía **[Origen: docs/DOMAIN_MODEL.md, decisión de aprobación 9, sin resolver]**

- **Descripción:** `docs/DOMAIN_MODEL.md` lista `Sale.status` como `CONFIRMED`/`VOIDED`, pero deja explícitamente pendiente de aprobación si la anulación de ventas se modela como ese estado o como un ajuste de inventario aparte. Al implementar el módulo `sales`, `SaleStatus` (Java) solo define `CONFIRMED` — no se presenta como resuelta una decisión que sigue abierta. `POST /sales/{id}/void` no se implementa (docs/API_DESIGN.md, sección 7.8, ya lo marca condicionado a esa aprobación).
- **Entidades afectadas:** `Sale`.
- **Validación:** no aplica — es una omisión deliberada, no una regla que el sistema imponga en tiempo de ejecución.
- **Error esperado:** no aplica.
- **Pruebas necesarias:** ninguna prueba de la suite depende de `VOIDED` ni de un endpoint de anulación.

### BR-032 — Clave de idempotencia de la solicitud de transferencia **[Decisión]**

- **Descripción:** la solicitud de transferencia (flujo C1) es una operación de creación repetible (categoría 2, `docs/CRITICAL_FLOWS.md` sección 1.1), por lo que necesita una clave de idempotencia — pero `docs/DOMAIN_MODEL.md` (sección 2.18) no listaba ninguna columna para ella. Se agrega `Transfer.client_reference_id` (texto, `UNIQUE`, nullable), exactamente el mismo patrón ya aprobado y aplicado en `Sale.client_reference_id` (decisión pendiente #5, resuelta en la fase de `sales`). Nulo para las transferencias que el propio sistema genera —las de reenvío del flujo F2—, que no provienen de una solicitud HTTP susceptible de doble clic.
- **Entidades afectadas:** `Transfer`.
- **Validación:** `TransferService.request` consulta la clave antes de crear nada; si ya existe, devuelve la transferencia original sin crear una segunda.
- **Error esperado:** 400 `IDEMPOTENCY_KEY_REQUERIDO` si falta el encabezado; un reintento con la misma clave **no** es un error (BR-017).
- **Pruebas necesarias:** doble envío con la misma clave no crea dos solicitudes; sin encabezado se rechaza.

### BR-033 — La cantidad aprobada no puede exceder la solicitada **[Decisión]**

- **Descripción:** al aprobar (flujo C2) la sucursal origen puede **ajustar la cantidad hacia abajo** (BR-005 lo contempla explícitamente: "aprobarla con una cantidad ajustada menor"), pero nunca hacia arriba — aprobar más de lo que el destino pidió no es un ajuste, es una transferencia distinta. Ni `docs/DOMAIN_MODEL.md` ni BR-005 lo decían de forma explícita; se formaliza aquí.
- **Entidades afectadas:** `TransferItem`.
- **Validación:** `TransferService.approve` compara contra `quantity_requested`, reforzado por `CHECK (quantity_approved IS NULL OR (quantity_approved > 0 AND quantity_approved <= quantity_requested))`. La cadena completa queda acotada en base de datos: `received ≤ shipped ≤ approved ≤ requested`.
- **Error esperado:** 422 `CANTIDAD_APROBADA_EXCEDE_SOLICITADO`.
- **Pruebas necesarias:** aprobar más de lo solicitado se rechaza; aprobar una cantidad menor se acepta y es la que limita el despacho.

### BR-034 — Aprobación y despacho cubren todas las líneas; la recepción admite subconjuntos **[Decisión]**

- **Descripción:** una transferencia tiene un **único tramo de envío** (`docs/DOMAIN_MODEL.md` 2.17; `docs/API_DESIGN.md` 7.9), así que el despacho no es parcial por línea: la solicitud debe incluir todas. Lo mismo se aplica a la aprobación, por una razón derivada: una línea sin `quantity_approved` no podría despacharse nunca (el despacho exige `shipped ≤ approved`), dejando la transferencia en un callejón sin salida. La **recepción**, en cambio, sí admite un subconjunto de líneas por llamada — el conteo físico puede hacerse línea por línea en momentos distintos (escenario 3.5), y el estado de la transferencia solo avanza cuando todas quedan atendidas.
- **Entidades afectadas:** `Transfer`, `TransferItem`.
- **Validación:** `TransferService.approve`/`dispatch` comparan el conjunto de líneas recibido contra el de la transferencia; `receive` no lo hace y recalcula el estado al final.
- **Error esperado:** 422 `APROBACION_INCOMPLETA`; 422 `DESPACHO_INCOMPLETO`; 404 `LINEA_TRANSFERENCIA_NO_ENCONTRADA` si se referencia una línea de otra transferencia; 422 `LINEA_DUPLICADA_EN_SOLICITUD` si la misma línea aparece dos veces.
- **Pruebas necesarias:** aprobar o despachar omitiendo una línea de una transferencia de dos se rechaza; recibir una sola línea de dos deja la transferencia en tránsito y la segunda recepción la cierra.

### BR-035 — Faltante nulo en recepción completa; recepción en cero no genera movimiento **[Decisión]**

- **Descripción:** resuelve la ambigüedad que BR-008 dejaba abierta ("`quantity_missing` en `NULL` o `0`, no ambiguo"): se persiste **`NULL` cuando la recepción fue completa** y la diferencia solo cuando realmente falta algo, tal como muestra el ejemplo 9.6 de `docs/API_DESIGN.md`. Así, "tiene faltante" es exactamente `quantity_missing IS NOT NULL`. Complemento: una línea recibida en **cero** (no llegó nada) es una recepción parcial válida (flujo F1) pero **no genera `InventoryMovement`** — un movimiento de cantidad cero violaría `CHECK (quantity > 0)` y no representa ningún hecho de stock.
- **Entidades afectadas:** `TransferItem`, `InventoryMovement`, `Inventory`.
- **Validación:** `TransferService.receive` guarda `null` si `received == shipped`, y solo toca inventario/ledger si `received > 0`.
- **Error esperado:** no aplica — ambos son caminos válidos, no rechazos.
- **Pruebas necesarias:** recepción completa deja `quantityMissing` nulo; recepción de 0 de N deja `quantityMissing = N`, sin fila de inventario ni movimiento en el destino.

---

## Ajustes pendientes al modelo de dominio (para aprobar antes de migrar)

1. ~~`Inventory.average_unit_cost`~~ — **resuelto:** aprobado y aplicado en `docs/DOMAIN_MODEL.md` (secciones 2.7, 3.2 y 6).
2. **Política de redondeo del costo promedio ponderado (BR-004):** aún no definida (ya señalada como pendiente en `docs/DOMAIN_MODEL.md`/`docs/STATUS.md`); se requiere decidir precisión decimal antes de implementar el cálculo.
3. ~~Mecanismo exacto de reintento de BR-022~~ — **resuelto:** definido en `docs/CRITICAL_FLOWS.md` (sección 1.2) como 3 intentos con backoff aleatorio corto.
4. ~~`PurchaseOrderItem.version`~~ — **resuelto:** aplicado al implementar el módulo `purchases` (migración V14). Bloqueo optimista manual (mismo patrón que `Inventory.version`, no `@Version` de JPA) sobre `quantity_received`, necesario porque es un agregado que puede incrementarse en varias recepciones parciales concurrentes sobre la misma línea (docs/CRITICAL_FLOWS.md, flujo B).
5. ~~`Sale.client_reference_id`~~ — **resuelto:** aplicado al implementar el módulo `sales` (migración V18). A diferencia de `InventoryMovement.idempotency_key` en la recepción de compra (BR-029, derivada por línea), aquí una sola clave por sucursal/solicitud basta: la venta completa (cabecera + todas sus líneas) se verifica de una sola vez, antes de procesar ninguna línea (docs/CRITICAL_FLOWS.md, flujo A).
6. ~~`InventoryMovement.idempotency_key`~~ — **parcialmente resuelto:** aplicado al implementar el módulo `purchases` (migración V15) para la recepción de compra (flujo B) — clave derivada `<Idempotency-Key del header>:<purchaseOrderItemId>`, ver BR-029. **Sigue pendiente** para el ajuste manual de inventario (flujo G, BR-023): la fase de `inventory` implementó ese endpoint sin idempotencia real (documentado en `docs/STATUS.md` como limitación conocida de esa fase) y no se retrofactoriza aquí — no era parte del alcance pedido para `purchases`.

7. **`Transfer.route_id`** (`docs/DOMAIN_MODEL.md` 2.18) — **no aplicado todavía:** la entidad `Route` pertenece al módulo `logistics`, explícitamente fuera del alcance de la fase de `transfers`. La columna y su FK se agregarán junto con esa tabla, igual que `sale_item_id`/`transfer_item_id` se agregaron a `inventory_movement` al implementar cada módulo que los originaba.

## Reglas ya señaladas como pendientes en documentos previos (no se resuelven aquí)

- Rol exacto que aprueba una transferencia y que decide el tratamiento del faltante (BR-005, BR-009): sigue como supuesto pendiente de confirmación (`docs/PROJECT_BRIEF.md`, `docs/USE_CASES.md`, `docs/DOMAIN_MODEL.md`). La implementación de `transfers` aplicó el supuesto ya registrado —aprobar/rechazar es del **Gerente de la sucursal origen**; el tratamiento del faltante, de un **Gerente de origen o destino**— sin darlo por confirmado.
- Si `Sale` admite anulación (`VOIDED`) o toda corrección se maneja como ajuste aparte (`docs/DOMAIN_MODEL.md`, decisión de aprobación 9) — afecta si BR-021 debe contemplar una excepción explícita para ese estado.

---

**Documentos relacionados:** `docs/DOMAIN_MODEL.md` (modelo de datos que estas reglas protegen), `docs/ARCHITECTURE.md` (capas, transacciones, concurrencia), `docs/adr/ADR-005-jwt-rbac.md` (BR-018), `docs/adr/ADR-008-trazabilidad-inventory-movement.md` (BR-001, BR-015).
