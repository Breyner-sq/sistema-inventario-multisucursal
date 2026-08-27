import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { listBranches } from "../../api/endpoints/branches";
import { getSale } from "../../api/endpoints/sales";
import { queryKeys } from "../../api/queryClient";
import { AsyncBoundary } from "../../components/state/states";
import { productLabel, useProductIndex, useUnitsOfMeasure } from "../products/useCatalog";

/** Comprobante de venta (RF-021): detalle consultable después de confirmada. */
export function SaleDetailPage() {
  const { id = "" } = useParams();
  const query = useQuery({ queryKey: queryKeys.sale(id), queryFn: () => getSale(id) });
  const branchesQuery = useQuery({ queryKey: queryKeys.branches({ active: true }), queryFn: () => listBranches({ active: true }) });
  const { byId: productsById } = useProductIndex();
  const { byId: unitsById } = useUnitsOfMeasure();

  return (
    <section>
      <div className="page__header">
        <h1>Comprobante de venta</h1>
        <Link to="/ventas">Volver a ventas</Link>
      </div>

      <AsyncBoundary isLoading={query.isPending} error={query.error} data={query.data} onRetry={() => query.refetch()}>
        {(sale) => (
          <>
            <dl className="detail-grid">
              <div><dt>Número</dt><dd>{sale.saleNumber}</dd></div>
              <div><dt>Sucursal</dt><dd>{branchesQuery.data?.content.find((b) => b.id === sale.branchId)?.name ?? sale.branchId}</dd></div>
              <div><dt>Fecha</dt><dd>{new Date(sale.saleDate).toLocaleString()}</dd></div>
              <div><dt>Estado</dt><dd><span className="badge badge--ok">{sale.status}</span></dd></div>
            </dl>

            <table>
              <thead>
                <tr>
                  <th scope="col">Producto</th>
                  <th scope="col">Cantidad</th>
                  <th scope="col">Unidad</th>
                  <th scope="col">Precio unitario</th>
                  <th scope="col">Descuento %</th>
                  <th scope="col">Total línea</th>
                </tr>
              </thead>
              <tbody>
                {sale.items.map((item, index) => (
                  <tr key={index}>
                    <td>{productLabel(productsById.get(item.productId), item.productId)}</td>
                    <td>{item.quantity}</td>
                    <td>{unitsById.get(item.unitOfMeasureId)?.code ?? item.unitOfMeasureId}</td>
                    <td>{item.unitPrice}</td>
                    <td>{item.discountPercentage}</td>
                    <td>{item.lineTotal}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <dl className="detail-grid">
              <div><dt>Subtotal</dt><dd>{sale.subtotal}</dd></div>
              <div><dt>Descuento</dt><dd>{sale.discountTotal}</dd></div>
              <div><dt>Total</dt><dd><strong>{sale.total}</strong></dd></div>
            </dl>
          </>
        )}
      </AsyncBoundary>
    </section>
  );
}
