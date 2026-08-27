import { apiRequest } from "../httpClient";
import type { Page, Supplier } from "../../types/api";

/** docs/API_DESIGN.md, sección 7.6. */

export function listSuppliers(params: { search?: string; active?: boolean; page?: number; size?: number } = {}): Promise<Page<Supplier>> {
  return apiRequest<Page<Supplier>>("/suppliers", { query: params });
}
