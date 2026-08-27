import type { Role } from "../types/api";

/**
 * Qué le mostramos a cada rol.
 *
 * <p><b>Esto es experiencia de usuario, no seguridad.</b> Ocultar una opción
 * evita que alguien llegue a una pantalla donde solo recibiría errores; no
 * impide nada. La autorización real vive en el backend
 * (`@PreAuthorize` + validación por sucursal, docs/API_DESIGN.md sección 6):
 * si un usuario fuerza la URL o llama a la API directamente, el servidor
 * responde 403 y la interfaz lo muestra como tal. Por eso ningún permiso se
 * "calcula" aquí más allá de lo necesario para armar el menú.
 *
 * <p>Las reglas de negocio (qué stock alcanza, si una transición es válida)
 * no se replican en el cliente en absoluto.
 */

export interface NavItem {
  label: string;
  path: string;
  /** Sin roles = visible para cualquier usuario autenticado. */
  roles?: Role[];
}

export const NAV_ITEMS: NavItem[] = [
  { label: "Inicio", path: "/" },
  { label: "Inventario", path: "/inventario" },
  { label: "Productos", path: "/productos" },
  { label: "Transferencias", path: "/transferencias" },
  { label: "Sucursales", path: "/sucursales" },
  { label: "Usuarios", path: "/usuarios", roles: ["ADMIN"] },
];

export function canSee(item: NavItem, role: Role | undefined): boolean {
  if (!item.roles) return true;
  return role !== undefined && item.roles.includes(role);
}

export function visibleNavItems(role: Role | undefined): NavItem[] {
  return NAV_ITEMS.filter((item) => canSee(item, role));
}

/**
 * Qué acciones ofrece la interfaz a cada rol. Es un espejo de la tabla de
 * autorización del backend (docs/API_DESIGN.md, sección 6) y sirve para no
 * ofrecer un botón que solo devolvería 403 — <b>no</b> para autorizar.
 * Si alguien llama a la API igualmente, el backend responde 403 y la pantalla
 * lo muestra tal cual.
 */
export const can = {
  /** POST/PATCH de productos y de sus unidades. */
  writeProducts: (role: Role | undefined): boolean => role === "OPERATOR" || role === "ADMIN",
  /** Alta en el catálogo global de unidades: más estricto que el resto del módulo. */
  createUnitOfMeasure: (role: Role | undefined): boolean => role === "ADMIN",
  /** Ajuste manual de inventario. */
  adjustInventory: (role: Role | undefined): boolean => role === "OPERATOR" || role === "ADMIN",
};

/**
 * Alcance por sucursal: `ADMIN` opera sobre cualquiera; el resto solo sobre la
 * suya (`AuthorizationService.requireBranchAccess`). La <i>lectura</i> de
 * inventario es abierta a cualquier sucursal (RF-003), así que esto aplica
 * únicamente a las escrituras.
 */
export function canWriteInBranch(user: { role: Role; branchId: string | null } | null, branchId: string | null): boolean {
  if (!user || !branchId) return false;
  if (user.role === "ADMIN") return true;
  return user.branchId === branchId;
}
