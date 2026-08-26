# Project Brief

## Objetivo
Sistema de gestión de inventario para múltiples sucursales.

## Stack aprobado
- Frontend: React + TypeScript
- Backend: Java 21 + Spring Boot
- Base de datos: PostgreSQL
- API: REST
- Persistencia: Spring Data JPA / Hibernate
- Seguridad: Spring Security + JWT + RBAC
- Arquitectura: monolito modular
- Infraestructura: Docker + Docker Compose
- Near-real-time: SSE o WebSocket solo cuando aporte valor

## Arquitectura
Frontend -> REST API -> Spring Boot -> PostgreSQL

## Principio rector
Cada decisión técnica debe poder responder:
"¿Por qué se hizo así?"

## Módulos
- Autenticación
- Sucursales
- Productos
- Inventario
- Movimientos
- Compras
- Ventas
- Transferencias
- Logística
- Dashboard
- Reportes

## Roles
- ADMIN
- MANAGER
- OPERATOR
