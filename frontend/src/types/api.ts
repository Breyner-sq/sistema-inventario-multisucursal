/**
 * Tipos del contrato REST v1.
 *
 * Están escritos a mano y no generados desde `docs/openapi.yaml` a propósito:
 * esa especificación es explícitamente parcial ("inicial y representativa, no
 * exhaustiva", sección 10 del propio documento), así que un cliente generado
 * quedaría incompleto justo en los recursos que no figuran en el YAML, con la
 * falsa apariencia de estar completo. Cuando la especificación cubra el 100%
 * del contrato, generarlos pasa a ser la mejor opción.
 *
 * Convenciones que se respetan aquí (docs/API_DESIGN.md, sección 1):
 * los identificadores viajan como `string`, y las fechas como ISO-8601 UTC.
 */

export type Role = "ADMIN" | "MANAGER" | "OPERATOR";

/** Sobre uniforme de error (docs/API_DESIGN.md, sección 3). */
export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
    status: number;
    requestId: string;
    details?: Array<{ field: string; issue: string }>;
  };
}

/** Sobre uniforme de paginación (docs/API_DESIGN.md, sección 1). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  role: Role;
  /** `null` para ADMIN: alcance global, sin sucursal fija. */
  branchId: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  user: UserSummary;
}

export interface Branch {
  id: string;
  code: string;
  name: string;
  location: string | null;
  active: boolean;
}

// ---- Productos y unidades de medida (docs/API_DESIGN.md, sección 7.4) ----

export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  baseUnitOfMeasureId: string;
  active: boolean;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  description?: string | null;
  baseUnitOfMeasureId: number;
}

export interface UpdateProductRequest {
  name: string;
  description?: string | null;
}

export interface UnitOfMeasure {
  id: string;
  code: string;
  name: string;
}

/** Unidad alternativa de un producto con su factor hacia la unidad base (BR-011). */
export interface ProductUnit {
  unitOfMeasureId: string;
  unitCode: string;
  unitName: string;
  /** Llega como número JSON; se maneja como string en formularios para no perder decimales. */
  conversionFactorToBase: number;
  baseUnit: boolean;
}

// ---- Inventario (docs/API_DESIGN.md, sección 7.5) ----

export interface InventoryRow {
  id: string;
  productId: string;
  branchId: string;
  quantityOnHand: number;
  averageUnitCost: number;
  minimumStock: number;
  updatedAt: string;
}

export type MovementDirection = "INGRESO" | "RETIRO";

export type MovementReason =
  | "COMPRA"
  | "DEVOLUCION"
  | "AJUSTE_INGRESO"
  | "VENTA"
  | "MERMA"
  | "AJUSTE_RETIRO"
  | "TRANSFERENCIA_SALIDA"
  | "TRANSFERENCIA_ENTRADA";

export interface InventoryMovement {
  id: string;
  productId: string;
  branchId: string;
  direction: MovementDirection;
  reason: MovementReason;
  quantity: number;
  unitOfMeasureId: string;
  responsibleUserId: string;
  occurredAt: string;
  notes: string | null;
  /** Documento que originó el movimiento; nulo en un ajuste manual (BR-023). */
  source: { type: "PURCHASE_ORDER" | "SALE" | "TRANSFER"; id: string } | null;
}

/**
 * Los motivos que un ajuste manual puede declarar, por dirección (BR-027).
 * El backend rechaza cualquier otra combinación con 422; aquí solo se limita
 * lo que ofrece el desplegable para no proponer algo que será rechazado.
 */
export const ADJUSTMENT_REASONS: Record<MovementDirection, MovementReason[]> = {
  INGRESO: ["AJUSTE_INGRESO", "DEVOLUCION"],
  RETIRO: ["AJUSTE_RETIRO", "MERMA"],
};

export interface InventoryAdjustmentRequest {
  branchId: number;
  productId: number;
  unitOfMeasureId?: number | null;
  direction: MovementDirection;
  reason?: MovementReason | null;
  quantity: number;
  notes: string;
}
