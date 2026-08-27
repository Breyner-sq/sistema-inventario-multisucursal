import { Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/layout/AppLayout";
import { ProtectedRoute, RequireRole } from "./routes/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { BranchesPage } from "./pages/BranchesPage";
import { ProductsPage } from "./pages/products/ProductsPage";
import { UnitsOfMeasurePage } from "./pages/products/UnitsOfMeasurePage";
import { InventoryPage } from "./pages/inventory/InventoryPage";
import { MovementsPage } from "./pages/inventory/MovementsPage";
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
          <Route path="/transferencias" element={<PlaceholderPage title="Transferencias" />} />

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
