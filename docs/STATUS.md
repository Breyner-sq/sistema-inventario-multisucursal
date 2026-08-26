

:contentReference[oaicite:3]{index=3}

---

## `STATUS.md`

```md
# Estado del Proyecto

**Estado general:** Inicialización

**Fase actual:** Preparación del proyecto antes de comenzar desarrollo con Claude.

# Completado

- [x] Revisión inicial de la prueba técnica.
- [x] Identificación de módulos principales.
- [x] Análisis de alternativas tecnológicas.
- [x] Selección del stack.
- [x] Selección de arquitectura inicial.
- [x] Diseño de estrategia de desarrollo asistido por IA.
- [x] Creación del repositorio.
- [x] Creación de estructura inicial de documentación.

# Stack aprobado

- React + TypeScript.
- Java 21 + Spring Boot.
- PostgreSQL.
- REST.
- Monolito modular.
- Spring Data JPA / Hibernate.
- Spring Security + JWT + RBAC.
- Docker + Docker Compose.
- SSE como primera opción near-real-time.

# Actualmente

No existe código productivo.

No se han creado todavía:

- frontend;
- backend;
- base de datos;
- Docker Compose.

Esto es intencional.

Primero se completarán las fases de análisis y diseño.

# Próximas fases

- [ ] Entregar Prompt Maestro a Claude.
- [ ] Trazabilidad completa de requisitos.
- [ ] Ingeniería de requisitos.
- [ ] Casos de uso.
- [ ] Matriz Actor × Acción.
- [ ] Validación de arquitectura.
- [ ] ADRs.
- [ ] Modelo de dominio.
- [ ] Modelo E-R.
- [ ] Reglas de negocio.
- [ ] Flujos críticos.
- [ ] Contrato REST.
- [ ] OpenAPI.

# Infraestructura pendiente

- [ ] Crear backend Spring Boot.
- [ ] Crear frontend React + TypeScript.
- [ ] Configurar PostgreSQL.
- [ ] Elegir Flyway o Liquibase.
- [ ] Crear Dockerfiles.
- [ ] Crear docker-compose.yml.
- [ ] Crear `.env.example`.
- [ ] Verificar `docker compose up`.

# Backend pendiente

- [ ] Seguridad.
- [ ] Usuarios.
- [ ] Sucursales.
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

- [ ] Arquitectura base.
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

- [ ] Tests unitarios.
- [ ] Tests de integración.
- [ ] Tests de API.
- [ ] Tests de seguridad.
- [ ] Tests de frontend.
- [ ] Tests E2E.
- [ ] Pruebas de concurrencia.
- [ ] Pruebas de idempotencia.
- [ ] Pruebas de rollback.

# Decisiones pendientes prioritarias

1. estrategia de concurrencia;
2. Inventory + InventoryMovement;
3. máquina de estados de transferencias;
4. momento del descuento/reserva de stock;
5. idempotencia;
6. herramienta de migraciones;
7. unidades de medida;
8. política monetaria.

# Problemas conocidos

Ninguno actualmente.

El desarrollo productivo todavía no ha comenzado.

# Próximo hito

Ejecutar con Claude:

## Prompt Maestro

y posteriormente:

## Fase 1 — Trazabilidad completa de requisitos

Resultado esperado:

```text
docs/REQUIREMENTS_TRACEABILITY.md
