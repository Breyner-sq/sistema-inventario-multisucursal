# Business Rules — Sistema de Inventario Multi-Sucursal

## 1. Propósito

Este documento define las reglas de negocio, invariantes y restricciones funcionales que deberán respetarse durante el diseño e implementación del Sistema de Inventario Multi-Sucursal.

El objetivo es evitar que reglas críticas queden implícitas en controladores, interfaces o decisiones aisladas de implementación.

Cada regla se clasifica como:

* **SOURCE:** proviene directamente de la prueba técnica.
* **DESIGN:** decisión adoptada para garantizar consistencia, seguridad, auditabilidad o mantenibilidad.
* **PENDING:** requiere análisis y aprobación antes de considerarse definitiva.

Las reglas `DESIGN` no deben presentarse como si fueran requisitos textuales de OptiPlant.

---

# 2. Principios generales

## BR-001 — La lógica de negocio pertenece al backend

**Tipo:** SOURCE

Las reglas que determinan si una operación puede realizarse o confirmarse deben ejecutarse en el backend.

El frontend puede realizar validaciones para mejorar la experiencia de usuario, pero dichas validaciones no sustituyen las validaciones del servidor.

---

## BR-002 — La base de datos es la fuente persistente de verdad

**Tipo:** DESIGN

PostgreSQL será la fuente persistente de verdad del sistema.

El estado mostrado en frontend o enviado mediante SSE deberá poder reconstruirse consultando nuevamente la API.

---

## BR-003 — Una operación confirmada debe quedar en estado coherente

**Tipo:** DESIGN

Las operaciones críticas que modifican varias entidades deben utilizar transacciones.

No se deberá confirmar parcialmente una operación que conceptualmente debe comportarse como una única unidad.

---

## BR-004 — Toda operación crítica debe ser auditable

**Tipo:** DESIGN basado en requisitos SOURCE

Las operaciones relevantes deberán conservar información suficiente para determinar:

* qué ocurrió;
* cuándo ocurrió;
* quién lo realizó;
* en qué sucursal ocurrió;
* qué recurso fue afectado;
* cuál fue el resultado.

---

# 3. Sucursales

## BR-010 — Independencia operativa por sucursal

**Tipo:** SOURCE

Cada sucursal debe poder gestionar sus operaciones locales de inventario de forma independiente.

---

## BR-011 — Visibilidad entre sucursales

**Tipo:** SOURCE

El sistema debe permitir consultar inventario de otras sucursales de la organización.

La capacidad exacta de consulta dependerá de las reglas de autorización definidas para cada rol.

---

## BR-012 — Identificación de sucursal

**Tipo:** DESIGN

Toda operación que afecte inventario deberá estar asociada explícitamente a una sucursal.

No debe inferirse una sucursal de forma ambigua cuando la operación involucre información sensible de inventario.

---

## BR-013 — Sucursal del usuario

**Tipo:** DESIGN

Cuando un usuario pertenezca a una sucursal concreta, esta relación podrá limitar las operaciones que puede ejecutar.

El alcance exacto se definirá en la matriz Actor × Acción.

---

## BR-014 — Eliminación de sucursales con historial

**Tipo:** PENDING

Una sucursal que posea ventas, compras, movimientos o transferencias históricas no debería eliminarse físicamente si ello rompe la integridad del historial.

Debe definirse posteriormente si se utilizará:

* desactivación;
* archivado;
* soft delete.

---

# 4. Productos

## BR-020 — Identificación única del producto

**Tipo:** DESIGN / PENDING

Cada producto deberá poseer un identificador técnico único.

Se evaluará además utilizar un identificador de negocio como:

* SKU;
* código interno.

La política de unicidad deberá definirse durante el modelado.

---

## BR-021 — Estado del producto

**Tipo:** DESIGN

Los productos podrán requerir un estado como:

* activo;
* inactivo.

Un producto con historial no deberá eliminarse arbitrariamente si ello impide consultar operaciones anteriores.

---

## BR-022 — Producto inactivo

**Tipo:** DESIGN / PENDING

Debe definirse qué operaciones puede realizar un producto inactivo.

Preferencia inicial:

* conservar consultas históricas;
* impedir nuevas operaciones comerciales que utilicen el producto.

---

# 5. Unidades de medida

## BR-030 — Múltiples unidades de medida

**Tipo:** SOURCE

El sistema debe permitir manejar múltiples unidades de medida por producto.

---

## BR-031 — Unidad base

**Tipo:** DESIGN / PENDING

Cada producto deberá tener una unidad base contra la cual puedan expresarse las conversiones.

Ejemplo conceptual:

```text
1 caja = 12 unidades
```

---

## BR-032 — Factor de conversión válido

**Tipo:** DESIGN

Un factor de conversión debe ser mayor que cero.

No se permitirán conversiones ambiguas o inválidas.

---

## BR-033 — Conversión determinista

**Tipo:** DESIGN

Dada una misma cantidad y unidad, el sistema debe producir siempre el mismo resultado de conversión.

---

## BR-034 — Precisión de cantidades

**Tipo:** DESIGN

Cuando una unidad admita cantidades fraccionarias deberán utilizarse tipos decimales apropiados.

No se utilizarán tipos de punto flotante imprecisos cuando puedan provocar errores acumulativos.

---

# 6. Inventario

## BR-040 — Inventario por sucursal y producto

**Tipo:** SOURCE + DESIGN

El inventario debe poder distinguir al menos:

```text
Sucursal + Producto
```

La combinación deberá identificar el saldo correspondiente a un producto dentro de una sucursal.

---

## BR-041 — Ingreso de inventario

**Tipo:** SOURCE

El sistema debe permitir registrar ingresos originados por:

* compras;
* devoluciones;
* ajustes de inventario;
* otros motivos explícitamente permitidos.

---

## BR-042 — Retiro de inventario

**Tipo:** SOURCE

El sistema debe permitir registrar retiros originados por:

* ventas;
* mermas;
* ajustes de inventario;
* otros motivos explícitamente permitidos.

---

## BR-043 — Cantidad válida

**Tipo:** DESIGN

Las cantidades utilizadas en operaciones de entrada o salida deben ser mayores que cero.

La dirección del movimiento debe estar determinada por el tipo de operación y no mediante cantidades negativas ambiguas.

---

## BR-044 — Trazabilidad obligatoria

**Tipo:** SOURCE

Cada ingreso o retiro deberá conservar como mínimo:

* fecha;
* responsable;
* motivo;
* cantidad.

---

## BR-045 — InventoryMovement

**Tipo:** DESIGN

Todo cambio de stock que corresponda a una operación auditable deberá producir un `InventoryMovement`.

El movimiento deberá contener, según corresponda:

* producto;
* sucursal;
* cantidad;
* tipo de movimiento;
* motivo;
* responsable;
* fecha;
* referencia a la operación que lo originó.

---

## BR-046 — Consistencia entre Inventory e InventoryMovement

**Tipo:** DESIGN

Si se mantiene un saldo materializado en `Inventory`, cualquier operación que modifique dicho saldo y requiera un movimiento deberá realizar ambos cambios dentro de la misma transacción.

No debe existir:

```text
stock actualizado
sin movimiento correspondiente
```

ni:

```text
movimiento confirmado
sin actualización de stock correspondiente
```

cuando la operación exija ambas acciones.

---

## BR-047 — Historial inmutable

**Tipo:** DESIGN

Un movimiento histórico confirmado no debe editarse o eliminarse mediante un CRUD normal.

Las correcciones deberán realizarse mediante un mecanismo explícito y auditable.

---

## BR-048 — Stock negativo

**Tipo:** PENDING / preferencia DESIGN

La política inicial es impedir que una operación confirmada deje stock negativo.

Esta regla deberá confirmarse formalmente durante el diseño del modelo de inventario.

---

## BR-049 — Stock mínimo

**Tipo:** SOURCE

El sistema debe permitir controlar un stock mínimo y detectar productos próximos a agotarse.

---

## BR-050 — Ajustes manuales

**Tipo:** SOURCE + DESIGN

El sistema puede registrar ajustes de inventario.

Todo ajuste deberá:

* tener responsable;
* registrar motivo;
* generar trazabilidad;
* respetar autorización;
* ejecutarse transaccionalmente.

---

## BR-051 — Ajustes no eliminan historial

**Tipo:** DESIGN

Un ajuste no deberá modificar silenciosamente movimientos anteriores.

Debe producir un nuevo movimiento que explique el cambio.

---

# 7. Concurrencia de inventario

## BR-060 — No vender stock inexistente

**Tipo:** SOURCE + DESIGN

La validación de stock debe realizarse al confirmar la operación.

Una lectura anterior realizada por frontend no garantiza que el stock continúe disponible.

---

## BR-061 — Operaciones concurrentes

**Tipo:** DESIGN

Dos operaciones concurrentes no pueden consumir conjuntamente más unidades de las disponibles.

Ejemplo:

```text
Stock inicial = 1

Venta A = 1
Venta B = 1
```

Resultado permitido:

```text
una operación confirmada
una operación rechazada
```

Resultado prohibido:

```text
dos operaciones confirmadas
stock = -1
```

---

## BR-062 — Estrategia técnica de concurrencia

**Tipo:** PENDING

Debe seleccionarse después de analizar los flujos críticos.

Opciones a evaluar:

* optimistic locking;
* pessimistic locking;
* actualización SQL atómica;
* estrategia combinada.

---

# 8. Compras

## BR-070 — Orden de compra

**Tipo:** SOURCE

El sistema debe permitir crear y gestionar órdenes de compra a proveedores.

---

## BR-071 — Datos comerciales

**Tipo:** SOURCE

La compra debe poder registrar:

* precio unitario;
* descuentos;
* plazo o condición de pago.

---

## BR-072 — Proveedor

**Tipo:** SOURCE

Las compras deben poder asociarse a proveedores para mantener histórico por proveedor.

---

## BR-073 — Histórico de compras

**Tipo:** SOURCE

Debe poder consultarse histórico:

* por proveedor;
* por producto.

---

## BR-074 — Recepción de mercancía

**Tipo:** SOURCE

El inventario solamente debe actualizarse cuando la recepción de mercancía sea confirmada.

Crear una orden de compra no implica que el producto ya exista físicamente en inventario.

---

## BR-075 — Recepción transaccional

**Tipo:** DESIGN

La recepción deberá ejecutarse conceptualmente como:

```text
validar recepción
→ registrar recepción/estado
→ calcular costo
→ incrementar inventario
→ registrar InventoryMovement
→ commit
```

Si un paso crítico falla antes del commit:

```text
rollback completo
```

---

## BR-076 — Costo promedio ponderado

**Tipo:** SOURCE

El sistema debe calcular el costo promedio ponderado del inventario.

Modelo conceptual:

```text
Costo nuevo =
((stock anterior × costo anterior)
+
(cantidad recibida × costo compra))
/
(stock anterior + cantidad recibida)
```

La fórmula definitiva deberá contemplar:

* descuentos;
* precisión;
* escala;
* política de redondeo.

---

## BR-077 — Stock anterior cero

**Tipo:** DESIGN

Cuando el stock anterior sea cero, el costo promedio deberá calcularse de forma coherente evitando divisiones inválidas.

---

## BR-078 — Recepción duplicada

**Tipo:** DESIGN

La misma recepción no deberá aplicarse dos veces por:

* doble clic;
* reintento HTTP;
* timeout;
* solicitud repetida.

El mecanismo concreto de idempotencia se definirá posteriormente.

---

# 9. Ventas

## BR-080 — Información de venta

**Tipo:** SOURCE

Toda venta deberá registrar:

* producto;
* cantidad;
* precio;
* sucursal;
* fecha;
* responsable.

---

## BR-081 — Validación de stock

**Tipo:** SOURCE

Debe validarse disponibilidad antes de confirmar la venta.

---

## BR-082 — Venta transaccional

**Tipo:** DESIGN

Confirmar una venta deberá ejecutar conceptualmente:

```text
validar solicitud
→ validar producto
→ validar stock
→ calcular precios/descuentos
→ crear venta
→ registrar líneas
→ disminuir inventario
→ crear InventoryMovement
→ commit
```

Un fallo intermedio debe producir rollback.

---

## BR-083 — Cantidad de venta

**Tipo:** DESIGN

La cantidad vendida debe ser mayor que cero.

---

## BR-084 — Precio de venta

**Tipo:** DESIGN

Una línea de venta debe conservar el precio aplicado al momento de la operación.

Un cambio posterior del precio del producto no debe modificar ventas históricas.

---

## BR-085 — Descuentos

**Tipo:** SOURCE + PENDING

El sistema debe permitir descuentos.

Debe definirse posteriormente:

* quién puede aplicarlos;
* límites;
* porcentaje o valor;
* combinaciones permitidas.

---

## BR-086 — Listas de precios

**Tipo:** SOURCE + PENDING

El sistema debe soportar diferentes listas de precios.

El modelo concreto deberá definirse durante el diseño de dominio.

---

## BR-087 — Comprobante

**Tipo:** SOURCE

Una venta confirmada debe conservar un registro consultable posteriormente.

---

## BR-088 — Doble confirmación

**Tipo:** DESIGN / PENDING

Debe evaluarse un mecanismo que evite crear una venta duplicada ante reintentos accidentales.

---

# 10. Transferencias entre sucursales

## BR-100 — Identificación de transferencia

**Tipo:** SOURCE

Toda transferencia deberá identificar:

* sucursal origen;
* sucursal destino;
* producto;
* cantidad solicitada.

---

## BR-101 — Origen diferente del destino

**Tipo:** DESIGN

Una transferencia debe involucrar sucursales diferentes.

No tiene sentido realizar una transferencia de una sucursal hacia sí misma.

---

## BR-102 — Solicitud

**Tipo:** SOURCE

La sucursal destino o un administrador puede generar una solicitud de transferencia.

---

## BR-103 — Revisión de disponibilidad

**Tipo:** SOURCE

La sucursal origen deberá revisar la disponibilidad antes de confirmar la transferencia.

---

## BR-104 — Ajuste de cantidad

**Tipo:** SOURCE

La sucursal origen puede:

* confirmar la cantidad solicitada;
* ajustar la cantidad según disponibilidad.

---

## BR-105 — Cantidad aprobada

**Tipo:** DESIGN

La cantidad aprobada deberá ser mayor que cero y no deberá superar las restricciones de disponibilidad aplicables.

---

## BR-106 — Preparación

**Tipo:** SOURCE

Debe existir una fase donde la sucursal origen prepare la mercancía.

---

## BR-107 — Despacho

**Tipo:** SOURCE

El despacho deberá conservar:

* fecha;
* transportista;
* fecha estimada de llegada.

---

## BR-108 — Máquina de estados

**Tipo:** DESIGN / PENDING

Las transferencias deberán seguir una máquina de estados explícita.

Modelo conceptual mínimo:

```text
REQUESTED
    ↓
APPROVED / PREPARING
    ↓
IN_TRANSIT
    ↓
RECEIVED
```

Debe contemplarse además:

```text
PARTIALLY_RECEIVED
```

Los nombres técnicos definitivos se establecerán posteriormente.

---

## BR-109 — Transiciones válidas

**Tipo:** DESIGN

No se permitirán saltos arbitrarios de estado.

Ejemplo:

Una transferencia no debería poder recibirse si nunca fue despachada.

---

## BR-110 — Afectación del stock origen

**Tipo:** PENDING

Debe definirse exactamente cuándo las unidades dejan de estar disponibles para otras operaciones.

Alternativas:

* reservar al aprobar;
* reservar al preparar;
* descontar al despachar;
* combinación reserva/descuento.

La decisión debe considerar concurrencia entre:

* ventas;
* transferencias.

---

## BR-111 — Despacho no puede exceder disponibilidad

**Tipo:** DESIGN

No se debe despachar más cantidad que la aprobada o disponible según la política definida.

---

## BR-112 — Despacho duplicado

**Tipo:** DESIGN

Una transferencia no debe descontar stock dos veces si el despacho es enviado nuevamente por error.

---

## BR-113 — Recepción completa

**Tipo:** SOURCE

Cuando la mercancía llegue completamente, el inventario destino debe actualizarse.

---

## BR-114 — Recepción parcial

**Tipo:** SOURCE

Debe permitirse registrar una recepción menor que la cantidad enviada.

---

## BR-115 — Cantidades de recepción

**Tipo:** DESIGN

Debe conservarse como mínimo:

* cantidad enviada;
* cantidad recibida;
* cantidad faltante.

---

## BR-116 — No recibir más de lo enviado

**Tipo:** DESIGN

En el flujo estándar:

```text
cantidad recibida <= cantidad enviada
```

Una corrección excepcional requeriría un flujo explícito y auditable.

---

## BR-117 — Faltantes

**Tipo:** SOURCE

Cuando exista recepción parcial deberá registrarse la diferencia.

---

## BR-118 — Tratamiento de faltante

**Tipo:** SOURCE

El sistema debe permitir definir tratamiento del faltante:

* reenvío;
* ajuste;
* reclamación.

---

## BR-119 — Recepción duplicada

**Tipo:** DESIGN

Una transferencia ya recibida no debe volver a incrementar inventario por un reintento accidental.

---

## BR-120 — Movimientos de transferencia

**Tipo:** DESIGN

Las afectaciones de inventario originadas por transferencias deberán producir movimientos auditables.

Deberá poder relacionarse el movimiento con la transferencia correspondiente.

---

# 11. Logística

## BR-130 — Tiempo estimado

**Tipo:** SOURCE

Debe registrarse o poder calcularse el tiempo estimado de entrega.

---

## BR-131 — Tiempo real

**Tipo:** SOURCE

Debe registrarse el momento real de entrega para comparar contra lo estimado.

---

## BR-132 — Estado de transferencia

**Tipo:** SOURCE

Debe poder visualizarse si una transferencia está:

* en preparación;
* en tránsito;
* recibida;
* con faltantes;

o el estado equivalente definido por la máquina de estados final.

---

## BR-133 — Clasificación de rutas

**Tipo:** SOURCE

Las rutas podrán clasificarse según:

* prioridad;
* costo;
* tiempo.

El modelo concreto será definido posteriormente.

---

## BR-134 — Única fuente de verdad del estado

**Tipo:** DESIGN

El módulo de logística no debe mantener un estado contradictorio con `Transfer`.

Debe existir una única fuente autoritativa para el estado de la transferencia.

---

## BR-135 — Cumplimiento logístico

**Tipo:** SOURCE

Debe poder generarse información de cumplimiento por:

* sucursal;
* ruta.

---

# 12. Dashboard y análisis

## BR-140 — Ventas comparativas

**Tipo:** SOURCE

Debe presentarse volumen de ventas del mes actual frente a meses anteriores.

---

## BR-141 — Rotación

**Tipo:** SOURCE + PENDING

Debe presentarse información sobre rotación de inventario.

La fórmula exacta deberá definirse antes de implementarla.

---

## BR-142 — Alta y baja demanda

**Tipo:** SOURCE + PENDING

Debe identificarse comportamiento de productos de:

* alta demanda;
* baja demanda.

Los criterios deberán definirse matemáticamente antes de implementarse.

---

## BR-143 — Transferencias activas

**Tipo:** SOURCE

El dashboard deberá mostrar estado de transferencias activas y su impacto sobre inventario.

---

## BR-144 — Reabastecimiento

**Tipo:** SOURCE

Debe permitir identificar productos próximos a agotarse.

---

## BR-145 — Comparación entre sucursales

**Tipo:** SOURCE

Los perfiles administrativos autorizados podrán visualizar comparaciones entre sucursales.

---

## BR-146 — Métricas derivadas de datos reales

**Tipo:** DESIGN

Los KPIs deberán calcularse desde datos persistidos.

No deberán utilizarse valores manuales que puedan contradecir las operaciones registradas.

---

## BR-147 — Fórmulas documentadas

**Tipo:** DESIGN

Antes de implementar cada KPI deberá documentarse:

* fórmula;
* fuente de datos;
* período;
* filtros;
* significado.

---

# 13. Usuarios y seguridad

## BR-150 — Autenticación

**Tipo:** DESIGN

Las operaciones protegidas requieren usuario autenticado.

---

## BR-151 — Roles iniciales

**Tipo:** DESIGN basado en actores SOURCE

Roles previstos:

```text
ADMIN
MANAGER
OPERATOR
```

---

## BR-152 — Administrador

**Tipo:** SOURCE

El administrador general tendrá visibilidad global y responsabilidades de configuración, usuarios y sucursales.

---

## BR-153 — Gerente

**Tipo:** SOURCE

El gerente supervisará operaciones de su sucursal, transferencias y reportes según permisos definitivos.

---

## BR-154 — Operador

**Tipo:** SOURCE

El operador podrá ejecutar las operaciones operativas definidas, incluyendo inventario, compras, ventas y solicitudes de transferencia dentro de su autorización.

---

## BR-155 — Autorización por recurso

**Tipo:** DESIGN

El acceso podrá depender de:

```text
rol
+
sucursal
+
recurso
+
operación
```

---

## BR-156 — Seguridad en backend

**Tipo:** DESIGN

Ocultar un botón en React no constituye autorización.

Toda operación protegida deberá verificarse nuevamente en backend.

---

## BR-157 — Responsable desde autenticación

**Tipo:** DESIGN

Cuando una operación deba registrar responsable, deberá utilizarse la identidad autenticada del usuario siempre que sea posible.

No debe confiarse únicamente en un `userId` suministrado libremente por el cliente.

---

# 14. Datos monetarios

## BR-160 — BigDecimal

**Tipo:** DESIGN

Los datos monetarios deberán utilizar tipos decimales exactos.

En Java:

```java
BigDecimal
```

No utilizar:

```java
float
double
```

para dinero.

---

## BR-161 — Redondeo

**Tipo:** PENDING

Debe definirse una política uniforme para:

* escala;
* redondeo;
* porcentajes;
* costo promedio;
* precios;
* descuentos.

---

## BR-162 — Histórico de precios

**Tipo:** DESIGN

Una operación comercial histórica debe conservar el precio aplicado en ese momento.

Modificar posteriormente una lista de precios no debe modificar ventas ya confirmadas.

---

# 15. Idempotencia

## BR-170 — Operaciones sensibles a reintentos

**Tipo:** DESIGN / PENDING

Debe evaluarse idempotencia explícita para:

* recepción de compras;
* ventas;
* despacho de transferencias;
* recepción de transferencias.

---

## BR-171 — Reintento HTTP

**Tipo:** DESIGN

Un reintento causado por timeout o fallo de red no debe producir una duplicación silenciosa de una operación crítica.

---

# 16. Near-real-time

## BR-180 — REST continúa siendo fuente consultable

**Tipo:** DESIGN

SSE o WebSocket sirven para notificar cambios.

No sustituyen a la API REST ni a PostgreSQL.

---

## BR-181 — Evento después del commit

**Tipo:** DESIGN

Los eventos que anuncien un cambio confirmado deben publicarse después del commit de la transacción.

---

## BR-182 — Fallo de comunicación

**Tipo:** DESIGN

Un fallo al enviar una actualización SSE no debe revertir una:

* venta;
* compra;
* transferencia;

que ya haya sido confirmada correctamente.

---

## BR-183 — Recuperación

**Tipo:** DESIGN

Si el cliente pierde una actualización, deberá poder recuperar el estado consultando nuevamente la API REST.

---

## BR-184 — Filtrado de eventos

**Tipo:** DESIGN

Un usuario no debe recibir mediante SSE información que no estaría autorizado a consultar mediante REST.

---

# 17. Alertas inteligentes de stock

## BR-190 — Generación de alerta

**Tipo:** DESIGN

La condición inicial candidata será:

```text
stock <= stockMinimo
```

Debe confirmarse antes de implementar.

---

## BR-191 — Alerta por sucursal

**Tipo:** DESIGN

Una alerta deberá identificar como mínimo:

* sucursal;
* producto;
* stock actual;
* umbral.

---

## BR-192 — Evitar duplicados

**Tipo:** DESIGN

Mientras un producto permanezca continuamente bajo el mismo umbral, no deberían generarse nuevas alertas idénticas sin necesidad.

---

## BR-193 — Recuperación de stock

**Tipo:** DESIGN / PENDING

Debe definirse qué ocurre cuando:

```text
stock > stockMinimo
```

Opciones posibles:

* cerrar automáticamente la alerta;
* marcarla como recuperada;
* mantener historial de alerta cerrada.

---

## BR-194 — Fallo de alerta

**Tipo:** DESIGN

Un fallo en la generación o notificación de una alerta no debe revertir una operación de inventario correctamente confirmada.

---

# 18. Eliminación y conservación histórica

## BR-200 — No romper historial

**Tipo:** DESIGN

Una entidad que participe en operaciones históricas no debe eliminarse físicamente si ello destruye o invalida trazabilidad.

---

## BR-201 — Estrategia por entidad

**Tipo:** PENDING

Debe decidirse individualmente cuándo utilizar:

* eliminación física;
* desactivación;
* archivado;
* soft delete.

No se aplicará soft delete indiscriminadamente a todas las tablas.

---

# 19. Errores de negocio

## BR-210 — Recurso inexistente

**Tipo:** DESIGN

Una operación sobre un recurso inexistente deberá producir un error claro y no crear datos implícitamente salvo que el caso de uso lo contemple expresamente.

---

## BR-211 — Conflicto

**Tipo:** DESIGN

Los conflictos de:

* concurrencia;
* estado;
* duplicación;
* transición inválida;

deberán distinguirse de errores de sintaxis o autenticación.

---

## BR-212 — Clasificación inicial HTTP

**Tipo:** DESIGN / PENDING

Baseline inicial:

```text
400 → solicitud inválida
401 → no autenticado
403 → no autorizado
404 → recurso inexistente
409 → conflicto de estado/concurrencia
422 → regla de negocio
500 → fallo inesperado
```

La clasificación definitiva se congelará durante el diseño de la API.

---

# 20. Reglas de auditoría

## BR-220 — Timestamp

**Tipo:** DESIGN

Las operaciones auditables deberán registrar timestamps utilizando una convención uniforme.

---

## BR-221 — Responsable

**Tipo:** SOURCE + DESIGN

Los movimientos de inventario deben registrar responsable.

---

## BR-222 — Motivo

**Tipo:** SOURCE

Los movimientos de ingreso o retiro deben registrar motivo.

---

## BR-223 — Referencia de operación

**Tipo:** DESIGN

Cuando un movimiento provenga de:

* compra;
* venta;
* transferencia;
* ajuste;

deberá poder identificarse la operación que lo originó.

---

# 21. Reportes

## BR-230 — Datos consistentes

**Tipo:** DESIGN

Los reportes deberán utilizar la misma fuente de datos que la aplicación.

Un reporte no deberá mostrar valores incompatibles con las consultas REST correspondientes.

---

## BR-231 — Autorización

**Tipo:** DESIGN

Los reportes deben respetar las mismas restricciones de rol y sucursal que las consultas normales.

---

## BR-232 — Rangos

**Tipo:** DESIGN

Cuando un reporte utilice fechas deberá validar:

```text
fechaInicio <= fechaFin
```

---

# 22. Invariantes críticas

Las siguientes invariantes tienen prioridad máxima durante desarrollo y testing:

### INV-001

No confirmar una venta sin stock disponible.

### INV-002

No permitir que operaciones concurrentes consuman más stock del disponible.

### INV-003

No modificar inventario sin conservar trazabilidad cuando la operación lo requiera.

### INV-004

No confirmar parcialmente una operación transaccional crítica.

### INV-005

No aplicar dos veces una recepción confirmada.

### INV-006

No aplicar dos veces un despacho confirmado.

### INV-007

No aplicar dos veces una recepción de transferencia.

### INV-008

No permitir transiciones inválidas de una transferencia.

### INV-009

No permitir que un usuario modifique recursos fuera de su autorización.

### INV-010

No eliminar historial necesario para auditoría.

### INV-011

No emitir una notificación como confirmada antes del commit de la operación que representa.

### INV-012

No utilizar valores monetarios de punto flotante impreciso.

---

# 23. Decisiones pendientes antes de implementación crítica

Antes de implementar completamente inventario, compras, ventas y transferencias deberán resolverse:

1. modelo definitivo de `Inventory`;
2. modelo definitivo de `InventoryMovement`;
3. estrategia de concurrencia;
4. política de stock negativo;
5. unidades de medida y conversiones;
6. herramienta de migraciones;
7. máquina de estados de transferencias;
8. momento de reserva/descuento de stock en transferencias;
9. estrategia de idempotencia;
10. política de eliminación/desactivación;
11. escala y redondeo monetario;
12. modelo de descuentos;
13. modelo de listas de precios;
14. modelo de proveedores;
15. definición matemática de KPIs;
16. comportamiento definitivo de alertas.

---

# 24. Regla de modificación

Las reglas marcadas como `SOURCE` no deberán eliminarse ni alterarse de manera que contradiga la prueba técnica.

Las reglas `DESIGN` podrán reconsiderarse únicamente si existe una razón técnica documentada.

Las reglas `PENDING` deberán resolverse mediante análisis antes de depender de ellas en código.

Cuando una decisión afecte de forma significativa la arquitectura o el dominio deberá registrarse también en:

```text
docs/DECISIONS.md
```

y, cuando corresponda:

```text
docs/adr/
```
