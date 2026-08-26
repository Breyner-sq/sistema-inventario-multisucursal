import axios from "axios";

/**
 * Cliente HTTP centralizado y configurable. La URL base se resuelve desde una
 * variable de entorno fijada en tiempo de build (VITE_API_BASE_URL, ver
 * .env.example en la raíz del repositorio) para no hardcodear el host del
 * backend. Punto único donde se agregarán, cuando existan los primeros
 * módulos de negocio, el interceptor de autenticación (JWT, ver
 * docs/adr/ADR-005-jwt-rbac.md) y el manejo uniforme de errores (ver
 * docs/API_DESIGN.md, sección 3).
 */
export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});
