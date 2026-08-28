import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { listBranches } from "../../api/endpoints/branches";
import { getLogisticsCompliance } from "../../api/endpoints/reports";
import { listRoutes } from "../../api/endpoints/routes";
import { queryKeys } from "../../api/queryClient";
import { useAuth } from "../../auth/useAuth";
import { AsyncBoundary } from "../../components/state/states";
import type { ComplianceMetrics } from "../../types/api";
import { ROUTE_CLASSIFICATION_LABELS } from "./routeLabels";

function formatRate(rate: number | null): string {
  return rate === null ? "—" : `${rate}%`;
}

function formatHours(hours: number | null): string {
  return hours === null ? "—" : `${hours} h`;
}

function MetricsRow({ metrics }: { metrics: ComplianceMetrics }) {
  return (
    <>
      <td>{metrics.dispatched}</td>
      <td>{metrics.delivered}</td>
      <td>{metrics.inTransit}</td>
      <td>{metrics.overdueInTransit}</td>
      <td>{metrics.onTime}</td>
      <td>{metrics.late}</td>
      <td>{metrics.withShortages}</td>
      <td>{formatRate(metrics.complianceRate)}</td>
      <td>{formatHours(metrics.averageDeliveryHours)}</td>
    </>
  );
}

/**
 * Cumplimiento logístico (RF-027, RF-030; BR-038). Todas las cifras vienen
 * calculadas por el backend a partir de columnas ya persistidas — esta
 * pantalla no computa ni reinterpreta ningún porcentaje, solo lo muestra.
 *
 * <p>Para `OPERATOR` la sucursal queda fija en la suya
 * (`LogisticsComplianceService` la fuerza igual que en compras/ventas); es la
 * única lectura del sistema donde `MANAGER` tampoco queda acotado a una
 * sucursal, por diseño del contrato (comparar sucursales es su propósito).
 */
export function LogisticsCompliancePage() {
  const { user } = useAuth();
  const isOperator = user?.role === "OPERATOR";

  const [branchId, setBranchId] = useState(isOperator ? user?.branchId ?? "" : "");
  const [routeId, setRouteId] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const branchesQuery = useQuery({ queryKey: queryKeys.branches({ active: true }), queryFn: () => listBranches({ active: true }) });
  const routesQuery = useQuery({ queryKey: queryKeys.routes({ size: 100 }), queryFn: () => listRoutes({ size: 100 }) });
  const branchesById = new Map((branchesQuery.data?.content ?? []).map((branch) => [branch.id, branch]));

  const params = {
    branchId: branchId || undefined,
    routeId: routeId || undefined,
    dateFrom: dateFrom ? `${dateFrom}T00:00:00Z` : undefined,
    dateTo: dateTo ? `${dateTo}T23:59:59Z` : undefined,
  };
  const query = useQuery({ queryKey: queryKeys.logisticsCompliance(params), queryFn: () => getLogisticsCompliance(params) });

  return (
    <section>
      <div className="page__header">
        <h1>Cumplimiento logístico</h1>
        <Link to="/logistica/rutas">Rutas</Link>
      </div>

      <form className="filters" onSubmit={(event) => event.preventDefault()}>
        <div className="field">
          <label htmlFor="compliance-branch">Sucursal</label>
          {isOperator ? (
            <p id="compliance-branch">{branchesById.get(branchId)?.name ?? "Tu sucursal"}</p>
          ) : (
            <select id="compliance-branch" value={branchId} onChange={(event) => setBranchId(event.target.value)}>
              <option value="">Todas</option>
              {(branchesQuery.data?.content ?? []).map((branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.name}
                </option>
              ))}
            </select>
          )}
        </div>
        <div className="field">
          <label htmlFor="compliance-route">Ruta</label>
          <select id="compliance-route" value={routeId} onChange={(event) => setRouteId(event.target.value)}>
            <option value="">Todas</option>
            {(routesQuery.data?.content ?? []).map((route) => (
              <option key={route.id} value={route.id}>
                {branchesById.get(route.originBranchId)?.name ?? route.originBranchId} → {branchesById.get(route.destinationBranchId)?.name ?? route.destinationBranchId}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="compliance-from">Despachadas desde</label>
          <input id="compliance-from" type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="compliance-to">Despachadas hasta</label>
          <input id="compliance-to" type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
        </div>
      </form>

      <AsyncBoundary isLoading={query.isPending} error={query.error} data={query.data} onRetry={() => query.refetch()}>
        {(report) => (
          <>
            <h2>Resumen</h2>
            <table>
              <thead>
                <tr>
                  <th scope="col">Despachadas</th>
                  <th scope="col">Entregadas</th>
                  <th scope="col">En tránsito</th>
                  <th scope="col">Atrasadas en curso</th>
                  <th scope="col">A tiempo</th>
                  <th scope="col">Tardías</th>
                  <th scope="col">Con faltante</th>
                  <th scope="col">% Cumplimiento</th>
                  <th scope="col">Horas promedio</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <MetricsRow metrics={report.summary} />
                </tr>
              </tbody>
            </table>

            <h2>Por ruta</h2>
            {report.byRoute.length === 0 ? (
              <p className="state__hint">No hay transferencias despachadas que coincidan con los filtros.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th scope="col">Ruta</th>
                    <th scope="col">Clasificación</th>
                    <th scope="col">Despachadas</th>
                    <th scope="col">Entregadas</th>
                    <th scope="col">En tránsito</th>
                    <th scope="col">Atrasadas en curso</th>
                    <th scope="col">A tiempo</th>
                    <th scope="col">Tardías</th>
                    <th scope="col">Con faltante</th>
                    <th scope="col">% Cumplimiento</th>
                    <th scope="col">Horas promedio</th>
                  </tr>
                </thead>
                <tbody>
                  {report.byRoute.map((route, index) => (
                    <tr key={route.routeId ?? `sin-clasificar-${index}`}>
                      <td>
                        {branchesById.get(route.originBranchId)?.name ?? route.originBranchId} →{" "}
                        {branchesById.get(route.destinationBranchId)?.name ?? route.destinationBranchId}
                      </td>
                      <td>{route.classification ? ROUTE_CLASSIFICATION_LABELS[route.classification] : "Sin clasificar"}</td>
                      <MetricsRow metrics={route.metrics} />
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}
      </AsyncBoundary>
    </section>
  );
}
