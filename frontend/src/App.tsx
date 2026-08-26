function App() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

  return (
    <main style={{ fontFamily: "sans-serif", maxWidth: 640, margin: "4rem auto", padding: "0 1rem" }}>
      <h1>Sistema de Inventario Multi-Sucursal</h1>
      <p>
        Esqueleto inicial del frontend (React + TypeScript). Todavía no implementa pantallas de
        negocio — ver <code>docs/STATUS.md</code>.
      </p>
      <p>
        API configurada en: <code>{apiBaseUrl}</code>
      </p>
    </main>
  );
}

export default App;
