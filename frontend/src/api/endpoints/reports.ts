import { apiRequest } from "../httpClient";
import type { LogisticsComplianceResponse } from "../../types/api";

/** docs/API_DESIGN.md, sección 7.10. */

export function getLogisticsCompliance(params: {
  branchId?: string;
  routeId?: string;
  dateFrom?: string;
  dateTo?: string;
}): Promise<LogisticsComplianceResponse> {
  return apiRequest<LogisticsComplianceResponse>("/reports/logistics-compliance", { query: params });
}
