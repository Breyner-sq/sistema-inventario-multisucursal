import { apiRequest } from "../httpClient";
import type { CreateSaleRequest, Page, Price, PriceList, Sale, SaleStatus } from "../../types/api";

/** docs/API_DESIGN.md, sección 7.8. */

export function listSales(params: {
  branchId?: string;
  status?: SaleStatus;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}): Promise<Page<Sale>> {
  return apiRequest<Page<Sale>>("/sales", { query: params });
}

export function getSale(id: string): Promise<Sale> {
  return apiRequest<Sale>(`/sales/${id}`);
}

export function createSale(body: CreateSaleRequest, idempotencyKey: string): Promise<Sale> {
  return apiRequest<Sale>("/sales", { method: "POST", body, idempotencyKey });
}

export function listPriceLists(params: { branchId?: string; active?: boolean } = {}): Promise<Page<PriceList>> {
  return apiRequest<Page<PriceList>>("/price-lists", { query: { ...params, size: 100 } });
}

export function listPrices(priceListId: string): Promise<Price[]> {
  return apiRequest<Price[]>(`/price-lists/${priceListId}/prices`);
}
