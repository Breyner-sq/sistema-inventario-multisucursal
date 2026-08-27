import { env } from "../config/env";
import { ApiError } from "./ApiError";
import { getStoredToken } from "../auth/session";
import type { ApiErrorBody } from "../types/api";

/**
 * Cliente HTTP centralizado: único lugar donde se arma una petición a la API.
 *
 * <p>Responsabilidades: resolver la URL base desde configuración (nunca
 * hardcodeada), adjuntar el token, normalizar toda falla a {@link ApiError} y
 * avisar cuando la sesión deja de ser válida.
 *
 * <p>Usa `fetch` nativo en vez de una librería HTTP: lo que necesitamos —JSON,
 * encabezados, cancelación— está cubierto por la plataforma, y así el manejo
 * de errores queda explícito en una función legible en lugar de repartido en
 * la configuración de interceptores de un tercero. Esto permitió además
 * eliminar `axios` de las dependencias.
 */

type UnauthorizedHandler = () => void;

let onUnauthorized: UnauthorizedHandler = () => {};

/**
 * Registra qué hacer cuando el servidor responde 401. Lo llama el proveedor de
 * sesión al montarse; el cliente HTTP no importa el contexto de React para
 * evitar una dependencia circular entre capas.
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  onUnauthorized = handler;
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
  /** Se serializan omitiendo `undefined` y `null`, para no mandar `?x=undefined`. */
  query?: Record<string, string | number | boolean | undefined | null>;
  signal?: AbortSignal;
  /** Obligatorio en las operaciones de creación repetible (docs/API_DESIGN.md, sección 2). */
  idempotencyKey?: string;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(`${env.apiBaseUrl}${path}`);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null) {
      url.searchParams.append(key, String(value));
    }
  }
  return url.toString();
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, query, signal, idempotencyKey } = options;

  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const token = getStoredToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    // No hubo respuesta: sin red, servidor caído o petición cancelada.
    throw ApiError.network(cause);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (response.ok) {
    return (await response.json()) as T;
  }

  let apiError: ApiError;
  try {
    apiError = ApiError.fromBody(response.status, (await response.json()) as ApiErrorBody);
  } catch {
    // El cuerpo no siguió el sobre uniforme (p. ej. un 502 en HTML de un proxy).
    apiError = ApiError.malformed(response.status);
  }

  if (apiError.isUnauthorized) {
    // El token expiró o dejó de ser válido: la sesión local ya no sirve.
    // Un 403 NO entra aquí — ahí la sesión es válida, solo faltan permisos.
    onUnauthorized();
  }

  throw apiError;
}
