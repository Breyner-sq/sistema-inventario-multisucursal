# sistema-inventario-multisucursal

Sistema de gestión de inventario multi-sucursal con control de stock, compras, ventas, transferencias, trazabilidad y análisis de información.

## Estado del proyecto

Fase actual: **esqueleto de repositorio** (frontend, backend y base de datos levantan con Docker Compose y tienen conectividad básica entre sí). No hay módulos de negocio implementados todavía (Product, Inventory, Sales, Purchases, Transfers) — ver [`docs/STATUS.md`](docs/STATUS.md) para el detalle de fases.

Toda decisión de arquitectura, requisitos y reglas de negocio está documentada en [`docs/`](docs/) antes de haberse implementado en código — empezar por [`docs/PROJECT_BRIEF.md`](docs/PROJECT_BRIEF.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) y [`docs/DECISIONS.md`](docs/DECISIONS.md).

## Stack

- Frontend: React + TypeScript (Vite).
- Backend: Java 21 + Spring Boot (Maven).
- Base de datos: PostgreSQL.
- Migraciones: Flyway.
- Infraestructura: Docker Compose.

Justificación de cada decisión en [`docs/DECISIONS.md`](docs/DECISIONS.md) y [`docs/adr/`](docs/adr/).

## Cómo levantar el proyecto

Requisitos: Docker y Docker Compose. No se necesita Java, Maven, Node ni PostgreSQL instalados localmente — todo corre dentro de los contenedores.

```bash
cp .env.example .env
docker compose up --build
```

Servicios expuestos (puertos configurables en `.env`):

| Servicio | URL por defecto |
|---|---|
| Frontend | http://localhost:3000 |
| Backend (API + health) | http://localhost:8080 |
| Health check del backend | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |

Para detener y liberar los contenedores:

```bash
docker compose down
```

Para eliminar también el volumen de datos de PostgreSQL:

```bash
docker compose down -v
```

## Desarrollo local sin Docker (opcional)

- **Backend:** requiere Java 21 y Maven 3.9+. Con una instancia de PostgreSQL accesible en `localhost:5432` (usar las variables de `.env`):
  ```bash
  cd backend
  mvn spring-boot:run
  ```
  Corre con el perfil `dev` por defecto (`spring.profiles.active=dev`, ver `application.yml`).

- **Frontend:** requiere Node 20+.
  ```bash
  cd frontend
  npm install
  npm run dev
  ```
  Sirve en http://localhost:5173 con recarga en caliente; usa `VITE_API_BASE_URL` de un archivo `.env` local en `frontend/` si se necesita apuntar a un backend distinto del valor por defecto.

## Pruebas

```bash
cd backend
mvn test
```

Ejecuta el arranque de contexto de Spring y un smoke test del endpoint `/actuator/health` (con una base de datos H2 en memoria, no PostgreSQL — ver la nota en `backend/pom.xml` sobre por qué las pruebas de módulos de negocio futuros deben usar PostgreSQL real vía Testcontainers).

## Documentación

Todo el proceso de diseño (requisitos, arquitectura, modelo de dominio, reglas de negocio, flujos críticos, contrato de API) vive en [`docs/`](docs/). El uso de IA durante el desarrollo está documentado en [`docs/AI_USAGE.md`](docs/AI_USAGE.md).
