# Arquitectura del Sistema de Inventario Multi-Sucursal

## 1. Objetivos arquitectónicos

La arquitectura debe priorizar:

- consistencia del inventario;
- trazabilidad;
- seguridad;
- transacciones;
- claridad de responsabilidades;
- mantenibilidad;
- simplicidad operacional;
- facilidad de despliegue;
- capacidad de evolución.

## 2. Arquitectura general

```text
React + TypeScript
        |
        | REST
        v
Java 21 + Spring Boot
        |
        | JPA / Hibernate
        v
PostgreSQL
