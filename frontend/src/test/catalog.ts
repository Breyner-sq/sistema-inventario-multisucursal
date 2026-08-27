import { jsonResponse } from "./harness";
import type { InventoryMovement, InventoryRow, Product, ProductUnit, UnitOfMeasure } from "../types/api";

/** Datos de catálogo compartidos por las pruebas de productos e inventario. */

export const UNITS: UnitOfMeasure[] = [
  { id: "1", code: "UND", name: "Unidad" },
  { id: "2", code: "CJA", name: "Caja" },
];

export const PRODUCTS: Product[] = [
  { id: "10", sku: "SKU-001", name: "Cemento gris", description: "Saco de 50 kg", baseUnitOfMeasureId: "1", active: true },
  { id: "11", sku: "SKU-002", name: "Arena fina", description: null, baseUnitOfMeasureId: "1", active: false },
];

export const PRODUCT_UNITS: ProductUnit[] = [
  { unitOfMeasureId: "1", unitCode: "UND", unitName: "Unidad", conversionFactorToBase: 1, baseUnit: true },
  { unitOfMeasureId: "2", unitCode: "CJA", unitName: "Caja", conversionFactorToBase: 12, baseUnit: false },
];

export const BRANCHES = [
  { id: "1", code: "SUC-001", name: "Sucursal Centro", location: "Centro", active: true },
  { id: "2", code: "SUC-002", name: "Sucursal Norte", location: "Norte", active: true },
];

export function inventoryRow(overrides: Partial<InventoryRow> = {}): InventoryRow {
  return {
    id: "100",
    productId: "10",
    branchId: "1",
    quantityOnHand: 5,
    averageUnitCost: 12.5,
    minimumStock: 10,
    updatedAt: "2026-08-27T10:00:00Z",
    ...overrides,
  };
}

export function movement(overrides: Partial<InventoryMovement> = {}): InventoryMovement {
  return {
    id: "900",
    productId: "10",
    branchId: "1",
    direction: "INGRESO",
    reason: "AJUSTE_INGRESO",
    quantity: 10,
    unitOfMeasureId: "1",
    responsibleUserId: "1",
    occurredAt: "2026-08-27T10:00:00Z",
    notes: "Conteo físico",
    source: null,
    ...overrides,
  };
}

export function page<T>(content: T[], overrides: Partial<{ page: number; size: number; totalPages: number }> = {}) {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    ...overrides,
  };
}

/**
 * Responde a las consultas de catálogo que cualquier pantalla necesita
 * (sucursales, unidades, índice de productos). Devuelve `undefined` para que
 * cada prueba maneje lo que le interesa.
 */
export function catalogResponse(url: string): Response | undefined {
  if (url.includes("/branches")) return jsonResponse(200, page(BRANCHES));
  if (url.includes("/units-of-measure")) return jsonResponse(200, UNITS);
  if (/\/products\/\d+\/units/.test(url)) return jsonResponse(200, PRODUCT_UNITS);
  return undefined;
}
