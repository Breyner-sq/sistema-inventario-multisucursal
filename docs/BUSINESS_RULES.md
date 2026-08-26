

---

## `BUSINESS_RULES.md`

```md
# Reglas de Negocio

## Convenciones

Cada regla puede clasificarse como:

- **SOURCE:** proviene directamente de la prueba técnica.
- **DESIGN:** regla adoptada para proteger el sistema.
- **PENDING:** necesita una decisión posterior.

# Inventario

## BR-001 — Inventario por sucursal

**Tipo:** SOURCE

El inventario debe identificarse al menos por:

```text
Sucursal + Producto
