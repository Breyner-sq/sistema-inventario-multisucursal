# Estado del Proyecto

**Estado general:** En desarrollo — infraestructura, módulo `auth`, módulos `branches`/`users` (hasta el alcance de UC-14/UC-15), módulo `products`/unidades de medida, `inventory`/movimientos (ajuste manual) y `purchases`/`suppliers` (hasta el alcance de esta fase: orden, recepción, costo promedio ponderado — no ventas ni transferencias) implementados y verificados; `sales`, `transfers`, `logistics`, `dashboard` aún no implementados.

**Fase actual:** Diseño completo (requisitos, arquitectura, modelo de dominio, reglas de negocio, flujos críticos, contrato de API), esqueleto de repositorio, `auth`, `branches`/`users`, `products`/unidades de medida, `inventory`/movimientos y `purchases`/`suppliers` verificados end-to-end. Próximo: `sales` o `transfers` — no se avanza sin instrucción explícita ("No avances a ventas").

# Completado

- [x] Revisión inicial de la prueba técnica.
- [x] Identificación de módulos principales.
- [x] Análisis de alternativas tecnológicas.
- [x] Selección del stack.
- [x] Selección de arquitectura inicial.
- [x] Diseño de estrategia de desarrollo asistido por IA.
- [x] Creación del repositorio.
- [x] Creación de estructura inicial de documentación.
- [x] Matriz de trazabilidad de requisitos (`docs/REQUIREMENTS_TRACEABILITY.md`).
- [x] Especificación de requisitos (`docs/PROJECT_BRIEF.md`).
- [x] Actores y casos de uso, historias de usuario, matriz Actor×Acción (`docs/USE_CASES.md`).
- [x] Arquitectura técnica detallada (`docs/ARCHITECTURE.md`).
- [x] ADRs 001–008 (`docs/adr/`).
- [x] Modelo de dominio y diagrama E-R (`docs/DOMAIN_MODEL.md`).
- [x] Reglas de negocio (`docs/BUSINESS_RULES.md`, BR-001 a BR-029).
- [x] Flujos críticos en pseudocódigo y diagramas de actividad (`docs/CRITICAL_FLOWS.md`).
- [x] Diseño de la API REST v1 + OpenAPI inicial (`docs/API_DESIGN.md`, `docs/openapi.yaml`).
- [x] Esqueleto de repositorio: backend, frontend, Docker Compose, verificado end-to-end.
- [x] Componentes transversales del backend (errores, validación, auditoría, Jackson, correlation id, paginación).
- [x] Módulo `auth`: Spring Security + JWT + RBAC, modelo mínimo de `User`/`Role`/`Branch`, autorización por rol y por sucursal, migraciones V1–V4, verificado end-to-end contra PostgreSQL real.
- [x] Módulos `branches`/`users` (UC-14, UC-15): CRUD de sucursales (lectura abierta, escritura ADMIN), gestión de usuarios (ADMIN-only) con asociación a sucursal, validaciones de unicidad/consistencia rol-sucursal, restricción de desactivar sucursal con usuarios activos, paginación (`docs/API_DESIGN.md`), migración V5, verificado end-to-end.
- [x] Módulo `products` + unidades de medida (RF-005/RF-011): catálogo de unidades de medida (lectura abierta, alta ADMIN-only), CRUD de productos (lectura abierta, escritura OPERATOR/ADMIN) sin campo de stock, unidad base creada automáticamente con factor 1 al crear el producto, unidades alternativas con factor de conversión (`BigDecimal`/`NUMERIC(19,6)`, positivo, validado en el payload), unidad base inmutable (422 `UNIDAD_BASE_INMUTABLE`), sin borrado físico (solo activar/desactivar), migraciones V6–V9, verificado end-to-end.
- [x] Módulo `inventory` + movimientos (RF-002/RF-003/RF-007 a RF-009; BR-001, BR-011, BR-012, BR-018, BR-021 a BR-023, BR-027): stock materializado por producto/sucursal (`Inventory.quantity_on_hand` en unidad base) con `InventoryMovement` como ledger append-only e inmutable; ajuste manual (`POST /inventory/adjustments`) con motivo/responsable/fecha/cantidad/tipo obligatorios, conversión de unidad alternativa hacia la base, sin stock negativo (`CHECK` + validación de aplicación), bloqueo optimista con reintento (hasta 3 intentos) sobre una columna `version` controlada manualmente (no `@Version` de JPA) para el patrón exacto de `docs/CRITICAL_FLOWS.md`; consulta de stock (`GET /inventory`, filtros `branchId`/`productId`/`search`/`lowStock`) y del ledger (`GET /inventory-movements`, filtros `branchId`/`productId`/`reason`/`dateFrom`/`dateTo`) abiertas a cualquier rol autenticado sobre cualquier sucursal; escritura restringida a OPERATOR (propia sucursal) + ADMIN; migraciones V10–V11; verificado end-to-end, incluyendo dos retiros concurrentes reales sobre el mismo stock y rollback ante fallo a mitad de transacción. No implementa `StockAlert`/alertas de stock mínimo (fuera del alcance explícito de esta fase) ni idempotencia por `Idempotency-Key` (columna `idempotency_key` sigue como decisión pendiente de aprobación, `docs/BUSINESS_RULES.md`).
- [x] **Corrección de bug preexistente descubierta en esta fase:** `GET /products` (y ahora `GET /inventory`) fallaban con 500 contra PostgreSQL real (no contra H2, usado en los tests) cuando no se enviaba el filtro `search` — PostgreSQL no puede inferir el tipo de un parámetro `NULL` dentro de `LOWER(CONCAT('%', ?, '%']))` y lo trata como `bytea`. Corregido con `CAST(:search AS string)` en `ProductRepository`/`InventoryRepository`. Un problema análogo con `Instant` nulos en `GET /inventory-movements` (dateFrom/dateTo) se resolvió resolviendo límites de fecha amplios por defecto en el servicio en vez de enviar `NULL` a la consulta — encontrado únicamente por verificación en vivo contra Postgres real, no por la suite de tests (H2 no reproduce este comportamiento de PostgreSQL).
- [x] Módulo `suppliers` (RF-012): ciclo mínimo de proveedor (razón social, identificación fiscal única, datos de contacto, baja/alta lógica); lectura abierta, escritura OPERATOR/ADMIN (misma convención que `products`); migración V12.
- [x] Módulo `purchases` (RF-012 a RF-016; BR-003, BR-004, BR-012, BR-016 a BR-019, BR-028, BR-029): orden de compra con líneas (producto, unidad, cantidad, precio unitario, descuento, `lineTotal` calculado — dinero en `NUMERIC(19,4)`, redondeo `HALF_UP` explícito), estados `CREATED → PARTIALLY_RECEIVED → RECEIVED` / `CREATED → CANCELLED` (solo sin recepciones), `orderNumber` generado por la aplicación (`OC-` + 8 caracteres de un UUID); recepción total o parcial (`POST /purchase-orders/{id}/receipts`) atómica en una única transacción — valida → aplica bloqueo optimista con reintento (hasta 3 intentos) sobre `PurchaseOrderItem.version` **y** sobre `Inventory.version` → recalcula el costo promedio ponderado (BR-004, usando el precio efectivamente recibido, no el pactado en la orden — BR-028) → incrementa stock → registra `InventoryMovement` (`reason=COMPRA`, enlazado a la línea de origen) → solo entonces recalcula y persiste el estado de la orden; idempotencia real por línea (`InventoryMovement.idempotency_key` derivada como `<Idempotency-Key>:<purchaseOrderItemId>`, BR-029) — un reintento de la misma recepción no duplica el efecto, incluso después de que la orden quedó `RECEIVED`; histórico consultable por proveedor (`GET /purchase-orders?supplierId=`) y por producto (`GET /inventory-movements?productId=&reason=COMPRA`, ya construido en la fase de `inventory`); lectura acotada a la propia sucursal para `MANAGER`/`OPERATOR` (`ADMIN` sin restricción), escritura `OPERATOR` (propia sucursal) + `ADMIN`; resuelve dos decisiones pendientes del modelo de dominio (`PurchaseOrderItem.version`, `InventoryMovement.idempotency_key` — parcial, ver `docs/BUSINESS_RULES.md`); migraciones V13–V15; verificado end-to-end, incluyendo doble recepción/reintento y rollback ante fallo a mitad de transacción.
- [x] **Bugs reales encontrados y corregidos durante la verificación en vivo de esta fase** (ninguno visible en la suite H2): (1) el estado de `PurchaseOrder` se mutaba en memoria después de que un `UPDATE` atómico con `@Modifying(clearAutomatically = true)` ya había limpiado el contexto de persistencia dentro de la misma transacción, dejando la entidad `detached` — el cambio de estado nunca se persistía aunque la respuesta HTTP lo mostrara correctamente (solo se detectaba con una *relectura* fresca, no confiando en el cuerpo de la respuesta del propio POST); corregido guardando explícitamente la orden (`purchaseOrderRepository.save(order)`) tras recalcular su estado. (2) La comprobación de "orden ya recibida" (409) se evaluaba antes que la de idempotencia por línea, por lo que un reintento legítimo con la misma `Idempotency-Key` fallaba con 409 en vez de replicar el resultado original cuando la propia recepción original ya había dejado la orden en `RECEIVED`; corregido reordenando las comprobaciones para que la idempotencia se compruebe primero, igual que en el pseudocódigo aprobado de `docs/CRITICAL_FLOWS.md`.

# Stack aprobado

- React + TypeScript.
- Java 21 + Spring Boot.
- PostgreSQL.
- REST.
- Monolito modular.
- Spring Data JPA / Hibernate.
- Flyway (migraciones — ver justificación en `backend/pom.xml` y `docs/adr/`).
- Spring Security + JWT + RBAC (implementado, módulo `auth` — ver ADR-005).
- Docker + Docker Compose.
- SSE como primera opción near-real-time.

# Actualmente

Existe infraestructura + autenticación/autorización + gestión de sucursales/usuarios + catálogo de productos/unidades de medida + inventario/movimientos (ajuste manual) + proveedores/compras (orden y recepción con costo promedio ponderado) funcionales, sin `sales`/`transfers`/`logistics`/`dashboard` todavía:

- **Backend:** Spring Boot 3.3.5 / Java 21, con `web`, `validation`, `data-jpa`, driver PostgreSQL, Flyway (V1–V15: `role`, `branch` [+ `location`], `users`, seed de usuarios de prueba, `unit_of_measure` [+ seed UN/CJ/KG], `product`, `product_unit`, `inventory`, `inventory_movement`, `supplier`, `purchase_order`, `purchase_order_item`, columnas de recepción/idempotencia en `inventory_movement`), `actuator` (`/actuator/health`) y `security` (JWT propio, sin OAuth2 externo — ver ADR-005). Componentes transversales (`common/`), módulo `auth` (login, `/auth/me`, filtro JWT, `AuthorizationService`), módulos `branches`/`users` completos hasta el alcance de UC-14/UC-15, módulo `products` (CRUD, unidades de medida, conversiones), módulo `inventory` (stock, ledger de movimientos, ajuste manual), módulo `suppliers` (ciclo mínimo de proveedor) y módulo `purchases` (orden de compra con líneas/descuentos, recepción atómica total/parcial, costo promedio ponderado, idempotencia real por línea). 127 tests automatizados (2 se omiten con gracia sin Docker-en-Docker).
- **Frontend:** React + TypeScript (Vite), sin pantallas de negocio; cliente HTTP (`axios`) configurado vía `VITE_API_BASE_URL`, listo para futuros interceptores de auth/errores.
- **Docker Compose:** 3 servicios (`postgres`, `backend`, `frontend`) con healthchecks, red y volumen de datos propios; verificado de punta a punta con las migraciones aplicadas contra PostgreSQL real y login/JWT/RBAC/CRUD de sucursales, usuarios, productos, inventario y compras (orden con descuento, recepción total/parcial, costo promedio con stock previo, doble recepción/reintento tras `RECEIVED`, permisos) probados en vivo (`curl`) — esta verificación en vivo encontró y corrigió bugs reales en cada fase que la suite basada en H2 no detectó (ver sección "Problemas conocidos").

No existen todavía: entidades JPA de `Sale`, `Transfer`, `StockAlert`, migraciones ni controladores de esos módulos, ni pantallas de frontend. Esto es intencional — condición de parada explícita de esta fase ("No avances a ventas").

# Próximas fases

- [x] Entregar Prompt Maestro a Claude.
- [x] Trazabilidad completa de requisitos.
- [x] Ingeniería de requisitos.
- [x] Casos de uso.
- [x] Matriz Actor × Acción.
- [x] Validación de arquitectura.
- [x] ADRs.
- [x] Modelo de dominio.
- [x] Modelo E-R.
- [x] Reglas de negocio.
- [x] Flujos críticos.
- [x] Contrato REST.
- [x] OpenAPI.
- [x] Implementación del módulo `auth` (Spring Security + JWT + RBAC real).
- [x] Implementación del módulo `products` (productos + unidades de medida + conversiones).
- [x] Implementación del módulo `inventory` (stock, movimientos, ajuste manual).
- [x] Implementación de los módulos `suppliers`/`purchases` (proveedor, orden, recepción, costo promedio ponderado).
- [ ] Implementación de los módulos de negocio restantes (sales, transfers, logistics, dashboard).
- [ ] Frontend: pantallas por módulo.

# Infraestructura pendiente

- [x] Crear backend Spring Boot.
- [x] Crear frontend React + TypeScript.
- [x] Configurar PostgreSQL.
- [x] Elegir Flyway o Liquibase → **Flyway**.
- [x] Crear Dockerfiles.
- [x] Crear docker-compose.yml.
- [x] Crear `.env.example`.
- [x] Verificar `docker compose up` (verificado end-to-end, ver sección "Actualmente").

# Backend pendiente

- [x] Componentes transversales: manejo global de excepciones, formato uniforme de error, Bean Validation, Jackson (fechas ISO-8601), auditoría base (`createdAt`/`updatedAt`/`createdBy`/`updatedBy`), correlation id + logging mínimo, paginación (`docs/ARCHITECTURE.md`, sección 8; `backend/src/main/java/com/inventario/multisucursal/common/`).
- [x] Seguridad (Spring Security + JWT + RBAC) — módulo `auth` completo: login, `/auth/me`, autorización por rol y por sucursal (`docs/adr/ADR-005-jwt-rbac.md`).
- [x] Usuarios/Sucursales — CRUD completo hasta el alcance de UC-14/UC-15 (`branches`: lectura abierta + escritura ADMIN, activar/desactivar con guarda de usuarios activos; `users`: ADMIN-only, asociación a sucursal, `GET /roles`).
- [x] Productos — CRUD completo (lectura abierta, escritura OPERATOR/ADMIN), sin stock, sin borrado físico, SKU único, unidad base inmutable.
- [x] Unidades — catálogo de unidades de medida (alta ADMIN-only) y unidades alternativas por producto con factor de conversión validado (`BigDecimal`/`NUMERIC(19,6)`, positivo, único índice parcial garantiza una sola unidad base por producto).
- [x] Inventario — stock materializado por producto/sucursal, `quantityOnHand`/`averageUnitCost`/`minimumStock` en `BigDecimal`, sin stock negativo, bloqueo optimista con reintento (BR-022).
- [x] Movimientos — ledger append-only (`InventoryMovement`), ajuste manual controlado (motivo/responsable/fecha/cantidad/tipo obligatorios, sin edición ni borrado), filtros y paginación (`branchId`/`productId`/`reason`/`dateFrom`/`dateTo`).
- [x] Proveedores — ciclo mínimo (razón social, identificación fiscal única, contacto, baja/alta lógica).
- [x] Compras — orden con líneas/descuentos/plazo de pago, estados (`CREATED`/`PARTIALLY_RECEIVED`/`RECEIVED`/`CANCELLED`), recepción atómica total/parcial con costo promedio ponderado e idempotencia real, histórico por proveedor y producto.
- [ ] Ventas.
- [ ] Transferencias.
- [ ] Logística.
- [ ] SSE.
- [ ] Dashboard.
- [ ] Alertas.
- [ ] Reportes.

# Frontend pendiente

- [ ] Arquitectura base (routing, layout).
- [ ] Login.
- [ ] Navegación.
- [ ] Productos.
- [ ] Inventario.
- [ ] Compras.
- [ ] Ventas.
- [ ] Transferencias.
- [ ] Logística.
- [ ] Dashboard.
- [ ] Alertas.

# Testing pendiente

- [x] Test de arranque de contexto + health del esqueleto backend.
- [x] Test del formato uniforme de error y de Bean Validation (`GlobalExceptionHandlerTest`).
- [x] Test de auditoría base (`AuditableEntityTest`).
- [x] Test de arranque de contexto + migraciones Flyway contra PostgreSQL real vía Testcontainers (`FlywayMigrationIntegrationTest`; se omite con gracia — `disabledWithoutDocker` — en entornos sin Docker realmente accesible, como el sandbox usado para esta verificación).
- [x] Tests del módulo `auth`: login válido/inválido, token ausente/expirado/manipulado, autorización por rol y por sucursal (`AuthenticationFlowTest`, 13 casos).
- [x] Tests de `branches`/`users`: creación válida, duplicados, permisos, asociación usuario-sucursal, acceso a sucursal ajena, recursos inexistentes (`BranchApiTest` 11 casos, `UserApiTest` 13 casos).
- [x] Tests de `products`: CRUD válido (con auto-creación de la unidad base), SKU duplicado, unidad de medida inválida (en creación de producto y al asociar unidad), factor de conversión inválido (no positivo → 400; edición de la unidad base → 422), unidad ya asociada (409), permisos (OPERATOR/ADMIN escriben productos; solo ADMIN crea unidades de medida; MANAGER sin permiso de escritura), producto inactivo (sigue siendo legible, filtro `active`, reactivación), recursos inexistentes (`ProductApiTest`, 16 casos).
- [x] Tests de `inventory`: entrada válida, retiro válido, retiro sin stock (422, sin movimiento creado), ajuste con motivo explícito y con motivo por defecto, motivo incompatible con la dirección (422), conversión de unidad alternativa hacia la base, cantidad no positiva (422), motivo (`notes`) faltante (400), movimiento auditable completo (responsable/fecha/motivo/cantidad/tipo), consulta de stock por sucursal desde cualquier rol, permisos (MANAGER sin acceso; OPERATOR limitado a su sucursal; ADMIN sin restricción), recursos inexistentes (`InventoryAdjustmentApiTest`, 16 casos); dos retiros concurrentes reales (hilos + transacciones independientes) sobre el mismo stock, verificando que exactamente uno se confirma y el stock final es exacto (`InventoryConcurrencyTest`); rollback del incremento de `Inventory` cuando falla la inserción del `InventoryMovement` a mitad de la misma transacción (`InventoryAdjustmentRollbackTest`, con `InventoryMovementRepository` mockeado para forzar el fallo).
- [x] Tests de `suppliers`: creación válida, identificación fiscal duplicada (409), lectura abierta, activar/desactivar, actualización sin alterar la identificación fiscal, recurso inexistente (`SupplierApiTest`, 7 casos).
- [x] Tests de `purchases`: orden válida con `lineTotal` calculado, descuentos (parcial, 100% → total cero, >100% → 400), cantidad ordenada ≤0 (422), precio ≤0 (400), producto duplicado en la misma orden (422), proveedor/producto inexistente (404), `Idempotency-Key` faltante (400), permisos y sucursal (MANAGER sin acceso; OPERATOR limitado a su sucursal en creación/lectura/lista; ADMIN sin restricción), estados de la orden (cancelar `CREATED`, rechazar cancelar una orden ya no `CREATED`) (`PurchaseOrderApiTest`, 18 casos); recepción total y parcial (con segunda recepción que completa la orden — con relectura fresca del estado persistido, no solo el de la respuesta), movimiento auditable enlazado a la línea de origen (`source.type=PURCHASE_ORDER`), costo promedio ponderado con stock cero y con stock previo, doble recepción/reintento con la misma `Idempotency-Key` (incluyendo el caso límite de reintentar después de que la orden ya quedó `RECEIVED` por ese mismo envío), segunda recepción legítima con una clave distinta, cantidad que excede lo pendiente (422), cantidad ≤0 (422), orden ya recibida (409), línea inexistente (404), `Idempotency-Key` faltante (400), permisos y sucursal (`PurchaseReceiptApiTest`, 17 casos); rollback de `PurchaseOrderItem.quantityReceived` e `Inventory` cuando falla la inserción del `InventoryMovement` a mitad de la misma transacción, sin que el estado de la orden avance (`PurchaseReceiptRollbackTest`).
- [ ] Tests unitarios de reglas de negocio.
- [ ] Tests de integración con PostgreSQL real de módulos de negocio (Testcontainers — ver nota en `backend/pom.xml`).
- [ ] Tests de API.
- [ ] Tests de seguridad.
- [ ] Tests de frontend.
- [ ] Tests E2E.
- [x] Pruebas de concurrencia (`InventoryConcurrencyTest`; `purchases` reutiliza el mismo patrón de bloqueo optimista con reintento, ver arriba).
- [x] Pruebas de idempotencia (`purchases`: doble recepción/reintento real por línea, ver arriba). Sigue sin aplicar a `inventory/adjustments` — ver limitación conocida más abajo.
- [x] Pruebas de rollback (`InventoryAdjustmentRollbackTest`, `PurchaseReceiptRollbackTest`, ver arriba).

# Decisiones pendientes prioritarias

Ver la lista consolidada y ya priorizada en `docs/DOMAIN_MODEL.md` (sección 7) y `docs/BUSINESS_RULES.md` (sección final) — entre ellas: estrategia de clave primaria, `Sale.client_reference_id` (única columna de idempotencia aún sin resolver — `InventoryMovement.idempotency_key` y `PurchaseOrderItem.version` ya se aplicaron en esta fase), rol que aprueba transferencias y trata faltantes, política de redondeo del costo promedio ponderado (aplicada como `HALF_UP`/escala 6 en esta fase, pendiente de una confirmación de negocio formal), y si `Sale` admite el estado `VOIDED`.

# Problemas conocidos

Ninguno pendiente en la infraestructura, `auth`, `branches`, `users`, `products`, `inventory`, `suppliers` o `purchases` actuales (verificado end-to-end contra PostgreSQL real vía Docker Compose). Bugs reales encontrados y corregidos por esa misma verificación en vivo (ninguno detectado por la suite basada en H2): dos de inferencia de tipos `NULL` de PostgreSQL en la fase de `inventory` (ver commit de esa fase), y dos en esta fase de `purchases` — (1) una entidad `PurchaseOrder` mutada en memoria después de que un `UPDATE` atómico con `clearAutomatically = true` ya la había dejado `detached` dentro de la misma transacción, perdiendo silenciosamente el cambio de estado (el cuerpo de la respuesta del propio POST lo mostraba correctamente, ocultando el problema hasta una relectura fresca); (2) la comprobación de "orden ya recibida" evaluándose antes que la de idempotencia por línea, rompiendo el reintento legítimo de una recepción que ya había completado la orden. `JWT_SECRET` por defecto en `.env.example`/`application.yml` es solo para desarrollo local — debe reemplazarse en cualquier entorno real.

Limitaciones deliberadas, no defectos: `inventory/adjustments` sigue sin `Idempotency-Key` real (la columna ya existe desde esta fase, pero retrofactorizar ese endpoint no era parte del alcance pedido para `purchases`); `StockAlert`/alertas de stock mínimo (UC-16) siguen sin implementar; `Inventory.minimum_stock` sigue sin endpoint de escritura propio; la creación de orden de compra (`POST /purchase-orders`) exige el encabezado `Idempotency-Key` pero no deduplica reintentos (no hay columna de idempotencia prevista para `PurchaseOrder` en el modelo aprobado, y no estaba entre las pruebas mínimas pedidas — ver BR-029 y la nota en `PurchaseOrderService`); `orderNumber` se genera como `OC-` + 8 caracteres de un UUID (decisión propia, no derivada del id correlativo, para evitar una escritura en dos pasos).

El desarrollo de `sales`, `transfers`, `logistics` y `dashboard` todavía no ha comenzado.

# Próximo hito

Implementar `sales` o `transfers` (siguientes módulos de negocio según `docs/ARCHITECTURE.md` sección 4, que dependen de `inventory`) — no se avanza sin instrucción explícita. No se implementaron en esta fase por instrucción explícita ("No avances a ventas").
