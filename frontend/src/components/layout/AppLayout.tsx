import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../auth/useAuth";
import { visibleNavItems } from "../../auth/permissions";

/** Estructura común de las pantallas autenticadas: cabecera, navegación por rol y contenido. */
export function AppLayout() {
  const { user, signOut } = useAuth();
  const items = visibleNavItems(user?.role);

  return (
    <div className="app">
      <header className="app__header">
        <Link to="/" className="app__brand">
          Inventario Multi-Sucursal
        </Link>
        <nav aria-label="Navegación principal">
          <ul>
            {items.map((item) => (
              <li key={item.path}>
                <NavLink to={item.path} end={item.path === "/"}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="app__session">
          {user ? (
            <span>
              {user.name} · {user.role}
            </span>
          ) : null}
          <button type="button" onClick={signOut}>
            Cerrar sesión
          </button>
        </div>
      </header>
      <main className="app__content">
        <Outlet />
      </main>
    </div>
  );
}
