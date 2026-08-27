import { Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/layout/AppLayout";
import { ProtectedRoute, RequireRole } from "./routes/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { BranchesPage } from "./pages/BranchesPage";
import { ProductsPage } from "./pages/products/ProductsPage";
import { UnitsOfMeasurePage } from "./pages/products/UnitsOfMeasurePage";
import { InventoryPage } from "./pages/inventory/InventoryPage";
import { MovementsPage } from "./pages/inventory/MovementsPage";
import { PurchaseOrdersPage } from "./pages/purchases/PurchaseOrdersPage";
import { NewPurchaseOrderPage } from "./pages/purchases/NewPurchaseOrderPage";
import { PurchaseOrderDetailPage } from "./pages/purchases/PurchaseOrderDetailPage";
import { SalesPage } from "./pages/sales/SalesPage";
import { NewSalePage } from "./pages/sales/NewSalePage";
import { SaleDetailPage } from "./pages/sales/SaleDetailPage";
import { ForbiddenPage, HomePage, NotFoundPage, PlaceholderPage } from "./pages/SimplePages";

/**
 * Mapa de rutas. Todo lo que cuelga de `ProtectedRoute` exige sesión; lo que
 * además cuelga de `RequireRole` exige un rol — recordando siempre que ambas
 * guardas son de navegación, y que quien autoriza de verdad es el backend.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="/sucursales" element={<BranchesPage />} />
          <Route path="/inventario" element={<InventoryPage />} />
          <Route path="/inventario/movimientos" element={<MovementsPage />} />
          <Route path="/productos" element={<ProductsPage />} />
          <Route path="/productos/unidades" element={<UnitsOfMeasurePage />} />
          <Route path="/compras" element={<PurchaseOrdersPage />} />
          <Route path="/compras/:id" element={<PurchaseOrderDetailPage />} />
          <Route path="/ventas" element={<SalesPage />} />
          <Route path="/ventas/:id" element={<SaleDetailPage />} />
          <Route path="/transferencias" element={<PlaceholderPage title="Transferencias" />} />

          {/* Crear orden de compra o venta es una acción de escritura de pleno
              derecho, no una vista mixta como /productos o /inventario: sin
              esta guarda, un MANAGER llegaría por URL a un formulario que
              parece operativo y solo fallaría con 403 al enviarlo. */}
          <Route element={<RequireRole roles={["OPERATOR", "ADMIN"]} />}>
            <Route path="/compras/nueva" element={<NewPurchaseOrderPage />} />
            <Route path="/ventas/nueva" element={<NewSalePage />} />
          </Route>

          <Route element={<RequireRole roles={["ADMIN"]} />}>
            <Route path="/usuarios" element={<PlaceholderPage title="Usuarios" />} />
          </Route>

          <Route path="/sin-permiso" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
