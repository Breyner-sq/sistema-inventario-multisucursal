import { Link } from "react-router-dom";
import { useAuth } from "../auth/useAuth";

export function HomePage() {
  const { user } = useAuth();
  return (
    <section>
      <h1>Hola, {user?.name}</h1>
      <p>
        Sesión iniciada como <strong>{user?.role}</strong>
        {user?.branchId ? ` · sucursal ${user.branchId}` : " · alcance global"}.
      </p>
      <p>
        La arquitectura base del frontend está lista; las pantallas de negocio se construyen en fases
        siguientes (ver <code>docs/STATUS.md</code>).
      </p>
    </section>
  );
}

/** Marcador para rutas ya enrutadas cuya pantalla aún no se construye. */
export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section>
      <h1>{title}</h1>
      <p>Esta pantalla todavía no está implementada.</p>
    </section>
  );
}

export function ForbiddenPage() {
  return (
    <section>
      <h1>Sin permiso</h1>
      <p>Tu rol no tiene acceso a esta sección. Si crees que es un error, consulta con un administrador.</p>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}

export function NotFoundPage() {
  return (
    <section>
      <h1>Página no encontrada</h1>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}
