# Sistema de Inventario Multi-Sucursal

## 1. Objetivo

Diseñar y desarrollar una aplicación robusta para la gestión de inventario de múltiples sucursales pertenecientes a una misma organización.

Cada sucursal debe poder operar sus transacciones locales de forma independiente, manteniendo al mismo tiempo visibilidad sobre el inventario general de la organización y coherencia de datos con las demás sucursales.

El sistema debe cubrir inventario, compras, ventas, transferencias, logística, análisis y trazabilidad de operaciones.

## 2. Principio rector

Toda decisión relevante del proyecto debe poder responder claramente:

> ¿Por qué se hizo así?

La solución no será evaluada únicamente por su funcionamiento, sino también por:

- calidad del diseño;
- solidez de la arquitectura;
- claridad de la documentación;
- mantenibilidad;
- justificación de decisiones técnicas;
- uso crítico y responsable de herramientas de inteligencia artificial.

## 3. Actores

### Administrador general

- Gestiona usuarios.
- Gestiona sucursales.
- Gestiona configuración general.
- Tiene visibilidad global del sistema.

### Gerente de sucursal

- Supervisa las operaciones de su sucursal.
- Consulta reportes.
- Supervisa inventario.
- Participa en la aprobación y gestión de transferencias.

### Operador de inventario

- Registra ingresos.
- Registra retiros.
- Registra compras.
- Registra ventas.
- Solicita transferencias.
- Ejecuta operaciones de inventario autorizadas.

### Sistema externo

Actor opcional para futuras integraciones mediante API con ERP, POS u otros sistemas.

## 4. Módulos funcionales obligatorios

### Inventario

Debe permitir:

- administrar productos;
- consultar inventario de la propia sucursal;
- consultar inventario de otras sucursales;
- registrar ingresos;
- registrar retiros;
- registrar ajustes;
- controlar stock mínimo;
- manejar múltiples unidades de medida;
- mantener trazabilidad completa de los movimientos.

Cada movimiento debe registrar como mínimo:

- fecha;
- responsable;
- motivo;
- cantidad.

### Compras

Debe permitir:

- crear órdenes de compra;
- gestionar proveedores;
- registrar precios;
- registrar descuentos;
- registrar condiciones de pago;
- confirmar recepción;
- actualizar inventario;
- consultar histórico de compras;
- calcular costo promedio ponderado.

### Ventas

Debe permitir:

- registrar ventas;
- registrar productos y cantidades;
- registrar precios;
- asociar sucursal;
- asociar responsable;
- validar stock antes de confirmar;
- aplicar descuentos;
- gestionar listas de precios;
- consultar posteriormente la venta.

### Transferencias entre sucursales

Debe soportar:

1. solicitud;
2. revisión de disponibilidad;
3. ajuste o confirmación de cantidad;
4. preparación;
5. despacho;
6. registro de transportista;
7. fecha estimada;
8. recepción completa;
9. recepción parcial;
10. gestión de faltantes.

Ante recepción parcial debe conservarse la diferencia y permitir definir un tratamiento posterior como:

- reenvío;
- ajuste;
- reclamación.

### Logística

Debe permitir:

- tiempo estimado de entrega;
- tiempo real de entrega;
- estado de transferencias;
- información del transportista;
- clasificación de rutas;
- consultas y reportes logísticos.

### Dashboard

Debe incluir como mínimo:

- ventas del mes frente a meses anteriores;
- rotación de inventario;
- productos de alta demanda;
- productos de baja demanda;
- transferencias activas;
- impacto de transferencias;
- productos próximos a agotarse;
- indicadores de reabastecimiento;
- comparación entre sucursales para perfiles autorizados.

## 5. Funcionalidad adicional

La funcionalidad adicional prioritaria será:

### Sistema de alertas inteligentes de stock

El sistema deberá detectar productos que alcancen o caigan por debajo de su stock mínimo y mostrar alertas relevantes para los usuarios autorizados.

Se prioriza esta funcionalidad porque:

- aporta valor operativo directo;
- reutiliza información del inventario;
- complementa el control de stock mínimo;
- tiene un alcance razonable para la prueba.

La predicción de demanda podrá evaluarse únicamente si todos los requisitos obligatorios están completos.

## 6. Requisitos técnicos obligatorios

La solución debe mantener separación entre:

1. frontend;
2. backend;
3. base de datos.

El frontend debe comunicarse exclusivamente con el backend mediante una API.

La lógica de negocio debe residir en el backend.

Todo el proyecto debe poder levantarse utilizando Docker Compose.

## 7. Stack tecnológico adoptado

Estas tecnologías son decisiones del proyecto y no requisitos impuestos por la prueba.

### Frontend

- React
- TypeScript

### Backend

- Java 21
- Spring Boot

### Persistencia

- Spring Data JPA
- Hibernate

### Base de datos

- PostgreSQL

### API

- REST

### Arquitectura

- Monolito modular

### Seguridad

- Spring Security
- JWT
- RBAC

### Infraestructura

- Docker
- Docker Compose

### Near-real-time

Preferencia inicial:

- Server-Sent Events (SSE)

WebSocket solamente se utilizará si aparece una necesidad bidireccional que SSE no pueda cubrir correctamente.

## 8. Principios técnicos

- Priorizar consistencia de datos.
- Priorizar trazabilidad.
- Proteger las operaciones críticas mediante transacciones.
- No colocar lógica de negocio en el frontend.
- No introducir tecnologías sin una necesidad concreta.
- Evitar sobrearquitectura.
- Mantener módulos claramente separados.
- No hardcodear secretos.
- Mantener el proyecto reproducible mediante Docker Compose.
- Toda funcionalidad crítica debe estar respaldada por pruebas.

## 9. Fuera de alcance inicial

No se incorporarán inicialmente:

- microservicios;
- Kubernetes;
- Kafka;
- RabbitMQ;
- Redis;
- CQRS;
- Event Sourcing;
- infraestructura cloud compleja;
- machine learning avanzado.

Estas tecnologías solamente podrán incorporarse si posteriormente aparece una necesidad concreta que las justifique.

## 10. Riesgos principales

- ventas concurrentes sobre el mismo stock;
- stock negativo;
- inconsistencias entre inventario y movimientos;
- recepción duplicada de compras;
- despacho duplicado de transferencias;
- recepción duplicada de transferencias;
- recepción parcial incorrecta;
- pérdida de trazabilidad;
- permisos incorrectos entre sucursales;
- cálculo incorrecto del costo promedio;
- sobrearquitectura;
- expansión innecesaria del alcance.

## 11. Documentación prevista

Durante el desarrollo se crearán progresivamente:

- REQUIREMENTS_TRACEABILITY.md
- USE_CASES.md
- DOMAIN_MODEL.md
- API_DESIGN.md
- CRITICAL_FLOWS.md
- ADRs
- diagramas de ingeniería
- README final

## 12. Regla para herramientas de IA

Las herramientas de IA deben distinguir entre:

### Requisitos de origen

Provenientes de la prueba técnica.

### Decisiones aprobadas

Decisiones adoptadas para esta solución.

### Supuestos

Aspectos que todavía deben analizarse o confirmarse.

Una decisión aprobada no debe cambiarse sin:

1. explicar el problema;
2. presentar evidencia;
3. analizar alternativas;
4. explicar ventajas y desventajas;
5. describir impacto;
6. solicitar aprobación.
