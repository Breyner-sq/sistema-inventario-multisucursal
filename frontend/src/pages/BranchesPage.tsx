import { useQuery } from "@tanstack/react-query";
import { listBranches } from "../api/endpoints/branches";
import { queryKeys } from "../api/queryClient";
import { AsyncBoundary } from "../components/state/states";

/**
 * Única pantalla con datos reales de esta fase: existe como <b>referencia del
 * patrón</b> —consulta tipada + `AsyncBoundary` para carga/vacío/error— que
 * seguirán las pantallas de negocio. Deliberadamente de solo lectura y sin
 * filtros ni acciones: construir el resto de features no es parte de esta fase.
 */
export function BranchesPage() {
  const query = useQuery({
    queryKey: queryKeys.branches({ active: true }),
    queryFn: () => listBranches({ active: true }),
  });

  return (
    <section>
      <h1>Sucursales</h1>
      <AsyncBoundary
        isLoading={query.isPending}
        error={query.error}
        data={query.data}
        onRetry={() => query.refetch()}
        isEmpty={(page) => page.content.length === 0}
        emptyTitle="No hay sucursales activas registradas."
      >
        {(page) => (
          <table>
            <caption>{page.totalElements} sucursal(es)</caption>
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Nombre</th>
                <th scope="col">Ubicación</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((branch) => (
                <tr key={branch.id}>
                  <td>{branch.code}</td>
                  <td>{branch.name}</td>
                  <td>{branch.location ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </AsyncBoundary>
    </section>
  );
}
