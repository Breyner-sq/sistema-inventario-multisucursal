import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { RenderResult } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import App from "../App";
import { AuthProvider } from "../auth/AuthContext";
import type { LoginResponse, Role, UserSummary } from "../types/api";

/**
 * Monta la aplicación completa —router, caché y sesión— sobre un `fetch`
 * simulado. Se prueba a través de la interfaz real y no de piezas aisladas,
 * porque lo que interesa verificar es el comportamiento observable:
 * "el usuario no ve la sección", "se le devuelve al login".
 */
export function renderApp(initialPath = "/"): RenderResult {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

export function userOfRole(role: Role): UserSummary {
  return {
    id: "1",
    name: role === "ADMIN" ? "Admin General" : "Persona Usuaria",
    email: `${role.toLowerCase()}@test.local`,
    role,
    branchId: role === "ADMIN" ? null : "1",
  };
}

/** Deja una sesión ya iniciada, como tras recargar la página. */
export function seedSession(role: Role): UserSummary {
  const user = userOfRole(role);
  sessionStorage.setItem("inventario.accessToken", "token-de-prueba");
  sessionStorage.setItem("inventario.user", JSON.stringify(user));
  return user;
}

export function loginResponseFor(role: Role): LoginResponse {
  return { accessToken: "token-de-prueba", expiresIn: 3600, user: userOfRole(role) };
}

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export function apiErrorResponse(status: number, code: string, message: string): Response {
  return jsonResponse(status, { error: { code, message, status, requestId: "req-de-prueba" } });
}

/** Reemplaza `fetch` por una función que responde según la ruta pedida. */
export function mockFetch(handler: (url: string, init?: RequestInit) => Response | Promise<Response>) {
  const spy = vi.fn((input: RequestInfo | URL, init?: RequestInit) => Promise.resolve(handler(String(input), init)));
  vi.stubGlobal("fetch", spy);
  return spy;
}
