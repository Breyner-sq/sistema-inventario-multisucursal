# Estado del Proyecto

**Estado general:** En desarrollo — infraestructura, módulo `auth` y módulos `branches`/`users` (hasta el alcance de UC-14/UC-15) implementados y verificados; módulos de negocio (inventory, sales, purchases, transfers, etc.) aún no implementados.

**Fase actual:** Diseño completo (requisitos, arquitectura, modelo de dominio, reglas de negocio, flujos críticos, contrato de API), esqueleto de repositorio, `auth` y `branches`/`users` verificados end-to-end. Próximo: implementación del primer módulo de negocio (products/inventory).

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
- [x] Reglas de negocio (`docs/BUSINESS_RULES.md`, BR-001 a BR-023).
- [x] Flujos críticos en pseudocódigo y diagramas de actividad (`docs/CRITICAL_FLOWS.md`).
- [x] Diseño de la API REST v1 + OpenAPI inicial (`docs/API_DESIGN.md`, `docs/openapi.yaml`).
- [x] Esqueleto de repositorio: backend, frontend, Docker Compose, verificado end-to-end.
- [x] Componentes transversales del backend (errores, validación, auditoría, Jackson, correlation id, paginación).
- [x] Módulo `auth`: Spring Security + JWT + RBAC, modelo mínimo de `User`/`Role`/`Branch`, autorización por rol y por sucursal, migraciones V1–V4, verificado end-to-end contra PostgreSQL real.
- [x] Módulos `branches`/`users` (UC-14, UC-15): CRUD de sucursales (lectura abierta, escritura ADMIN), gestión de usuarios (ADMIN-only) con asociación a sucursal, validaciones de unicidad/consistencia rol-sucursal, restricción de desactivar sucursal con usuarios activos, paginación (`docs/API_DESIGN.md`), migración V5, verificado end-to-end.

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

Existe infraestructura + autenticación/autorización + gestión de sucursales/usuarios funcionales, sin módulos de negocio (inventario/compras/ventas/transferencias) todavía:

- **Backend:** Spring Boot 3.3.5 / Java 21, con `web`, `validation`, `data-jpa`, driver PostgreSQL, Flyway (V1–V5: `role`, `branch` [+ `location`], `users`, seed de usuarios de prueba), `actuator` (`/actuator/health`) y `security` (JWT propio, sin OAuth2 externo — ver ADR-005). Componentes transversales (`common/`), módulo `auth` (login, `/auth/me`, filtro JWT, `AuthorizationService`), y módulos `branches`/`users` completos hasta el alcance de UC-14/UC-15 (CRUD, activar/desactivar, paginación, `GET /roles`). 51 tests automatizados (2 se omiten con gracia sin Docker-en-Docker).
- **Frontend:** React + TypeScript (Vite), sin pantallas de negocio; cliente HTTP (`axios`) configurado vía `VITE_API_BASE_URL`, listo para futuros interceptores de auth/errores.
- **Docker Compose:** 3 servicios (`postgres`, `backend`, `frontend`) con healthchecks, red y volumen de datos propios; verificado de punta a punta con las migraciones aplicadas contra PostgreSQL real y login/JWT/RBAC/CRUD de sucursales y usuarios probados en vivo (`curl`).

No existen todavía: entidades JPA de negocio (Product, Inventory, Sale, Purchase, Transfer), migraciones ni controladores de esos módulos, ni pantallas de frontend. Esto es intencional — condición de parada explícita de esta fase.

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
- [ ] Implementación de los módulos de negocio (products, inventory, purchases, sales, transfers, logistics, dashboard).
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
- [ ] Productos.
- [ ] Unidades.
- [ ] Inventario.
- [ ] Movimientos.
- [ ] Compras.
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
- [ ] Tests unitarios de reglas de negocio.
- [ ] Tests de integración con PostgreSQL real de módulos de negocio (Testcontainers — ver nota en `backend/pom.xml`).
- [ ] Tests de API.
- [ ] Tests de seguridad.
- [ ] Tests de frontend.
- [ ] Tests E2E.
- [ ] Pruebas de concurrencia.
- [ ] Pruebas de idempotencia.
- [ ] Pruebas de rollback.

# Decisiones pendientes prioritarias

Ver la lista consolidada y ya priorizada en `docs/DOMAIN_MODEL.md` (sección 7) y `docs/BUSINESS_RULES.md` (sección final) — entre ellas: estrategia de clave primaria, columnas de idempotencia (`Sale.client_reference_id`, `InventoryMovement.idempotency_key`, `PurchaseOrderItem.version`), rol que aprueba transferencias y trata faltantes, política de redondeo del costo promedio ponderado, y si `Sale` admite el estado `VOIDED`.

# Problemas conocidos

Ninguno en la infraestructura, `auth`, `branches` o `users` actuales (verificado end-to-end contra PostgreSQL real vía Docker Compose). `JWT_SECRET` por defecto en `.env.example`/`application.yml` es solo para desarrollo local — debe reemplazarse en cualquier entorno real.

El desarrollo de módulos de negocio (products, inventory, sales, purchases, transfers, etc.) todavía no ha comenzado.

# Próximo hito

Implementar el primer módulo de negocio (`products`/`inventory`, los módulos base según `docs/ARCHITECTURE.md` sección 4) — no se avanza sin instrucción explícita. No se implementó `products` en esta fase por instrucción explícita ("no avances a productos").
