import { apiRequest } from "../httpClient";
import type { Branch, Page } from "../../types/api";

/**
 * docs/API_DESIGN.md, sección 7.3. Este módulo existe como referencia del
 * patrón que seguirán el resto de recursos: una función por endpoint, tipada
 * con el contrato, sin lógica de presentación ni de negocio.
 */
export function listBranches(params: { active?: boolean; page?: number; size?: number } = {}): Promise<Page<Branch>> {
  return apiRequest<Page<Branch>>("/branches", { query: params });
}
