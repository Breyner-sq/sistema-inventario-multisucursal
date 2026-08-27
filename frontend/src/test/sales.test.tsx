import { describe, expect, it } from "vitest";
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { apiErrorResponse, jsonResponse, mockFetch, renderApp, seedSession, selectOption } from "./harness";
import { PRODUCTS, catalogResponse, page, sale } from "./catalog";

function saleRoutes(overrides: (url: string, init?: RequestInit) => Response | Promise<Response> | undefined = () => undefined) {
  return (url: string, init?: RequestInit) => {
    const custom = overrides(url, init);
    if (custom) return custom;
    const catalog = catalogResponse(url);
    if (catalog) return catalog;
    if (url.includes("/products")) return jsonResponse(200, page(PRODUCTS));
    if (/\/sales\/\d+$/.test(url)) return jsonResponse(200, sale());
    if (url.includes("/sales")) return jsonResponse(200, page([sale()]));
    return jsonResponse(200, page([]));
  };
}

describe("Listado de ventas", () => {
  it("muestra la venta con sucursal, fecha y total", async () => {
    seedSession("OPERATOR");
    mockFetch(saleRoutes());
    renderApp("/ventas");

    const row = (await screen.findByText("V-ABC12345")).closest("tr")!;
    expect(within(row).getByText("Sucursal Centro")).toBeInTheDocument();
    expect(within(row).getByText("150")).toBeInTheDocument();
  });

  it("muestra el estado vacío cuando no hay ventas", async () => {
    seedSession("OPERATOR");
    mockFetch(saleRoutes((url) => (url.includes("/sales?") ? jsonResponse(200, page([])) : undefined)));
    renderApp("/ventas");

    expect(await screen.findByText(/no hay ventas registradas/i)).toBeInTheDocument();
  });

  describe("permisos visuales", () => {
    it("OPERATOR ve 'Nueva venta'", async () => {
      seedSession("OPERATOR");
      mockFetch(saleRoutes());
      renderApp("/ventas");

      expect(await screen.findByRole("button", { name: /nueva venta/i })).toBeInTheDocument();
    });

    it("MANAGER no ve 'Nueva venta' y forzar la URL lo envía a sin permiso", async () => {
      seedSession("MANAGER");
      mockFetch(saleRoutes());
      const { unmount } = renderApp("/ventas");

      expect(await screen.findByText("V-ABC12345")).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: /nueva venta/i })).not.toBeInTheDocument();
      unmount();

      renderApp("/ventas/nueva");
      expect(await screen.findByText(/sin permiso/i)).toBeInTheDocument();
    });
  });
});

describe("Nueva venta", () => {
  it("preselecciona la lista de precios de la sucursal sobre la global y previsualiza el precio", async () => {
    seedSession("OPERATOR");
    mockFetch(saleRoutes());
    renderApp("/ventas/nueva");

    await waitFor(() => expect(screen.getByLabelText(/lista de precios/i)).toHaveValue("1"));
    await selectOption(/producto de la línea 1/i, "10");

    const row = screen.getByLabelText(/producto de la línea 1/i).closest("tr")!;
    expect(await within(row).findByText("50")).toBeInTheDocument();
  });

  it("calcula el total de línea estimado con descuento", async () => {
    seedSession("OPERATOR");
    mockFetch(saleRoutes());
    renderApp("/ventas/nueva");

    await selectOption(/producto de la línea 1/i, "10");
    await userEvent.type(screen.getByLabelText(/cantidad de la línea 1/i), "4");
    await userEvent.type(screen.getByLabelText(/descuento de la línea 1/i), "10");

    // 4 * 50 = 200, menos 10% = 180
    const row = screen.getByLabelText(/producto de la línea 1/i).closest("tr")!;
    expect(await within(row).findByText("180.00")).toBeInTheDocument();
    expect(screen.getByText(/total estimado:/i)).toBeInTheDocument();
  });

  it("no revisa la venta si falta seleccionar un producto", async () => {
    seedSession("OPERATOR");
    const fetchSpy = mockFetch(saleRoutes());
    renderApp("/ventas/nueva");

    await userEvent.click(await screen.findByRole("button", { name: /revisar venta/i }));

    expect(await screen.findByText(/selecciona un producto/i)).toBeInTheDocument();
    expect(fetchSpy.mock.calls.some(([, init]) => (init as RequestInit | undefined)?.method === "POST")).toBe(false);
  });

  it("confirma el resumen y envía la venta con Idempotency-Key", async () => {
    seedSession("OPERATOR");
    const fetchSpy = mockFetch(
      saleRoutes((url, init) => {
        if (url.includes("/sales") && init?.method === "POST") return jsonResponse(201, sale({ id: "888" }));
        if (url.endsWith("/sales/888")) return jsonResponse(200, sale({ id: "888" }));
        return undefined;
      }),
    );
    renderApp("/ventas/nueva");

    await selectOption(/producto de la línea 1/i, "10");
    await userEvent.type(screen.getByLabelText(/cantidad de la línea 1/i), "3");
    await userEvent.click(screen.getByRole("button", { name: /revisar venta/i }));

    const dialog = await screen.findByRole("dialog", { name: /confirmar venta/i });
    await userEvent.click(within(dialog).getByRole("button", { name: /confirmar venta/i }));

    const post = await waitFor(() => {
      const call = fetchSpy.mock.calls.find(([url, init]) => String(url).includes("/sales") && (init as RequestInit)?.method === "POST");
      expect(call).toBeDefined();
      return call!;
    });
    const [, init] = post;
    expect((init as RequestInit).headers).toMatchObject({ "Idempotency-Key": expect.any(String) });
    expect(JSON.parse(String((init as RequestInit).body))).toMatchObject({
      branchId: 1,
      priceListId: 1,
      items: [{ productId: 10, quantity: 3 }],
    });
    expect(await screen.findByText(/comprobante de venta/i)).toBeInTheDocument();
  });

  it("muestra el stock insuficiente del backend, conserva el formulario y limpia el error al corregir", async () => {
    seedSession("OPERATOR");
    mockFetch(
      saleRoutes((url, init) =>
        url.includes("/sales") && init?.method === "POST"
          ? apiErrorResponse(422, "STOCK_INSUFICIENTE", "No hay stock suficiente. Disponible: 5.000000, solicitado: 999.000000.")
          : undefined,
      ),
    );
    renderApp("/ventas/nueva");

    await selectOption(/producto de la línea 1/i, "10");
    await userEvent.type(screen.getByLabelText(/cantidad de la línea 1/i), "999");
    await userEvent.click(screen.getByRole("button", { name: /revisar venta/i }));
    await userEvent.click(await screen.findByRole("button", { name: /confirmar venta/i }));

    expect(await screen.findByText(/no hay stock suficiente/i)).toBeInTheDocument();
    // El formulario sigue disponible: no se perdió la cantidad tecleada.
    expect(screen.getByLabelText(/cantidad de la línea 1/i)).toHaveValue("999");

    await userEvent.click(screen.getByRole("button", { name: /volver/i }));
    await userEvent.clear(screen.getByLabelText(/cantidad de la línea 1/i));
    await userEvent.type(screen.getByLabelText(/cantidad de la línea 1/i), "2");
    await userEvent.click(screen.getByRole("button", { name: /revisar venta/i }));

    const confirmation = await screen.findByRole("dialog", { name: /confirmar venta/i });
    expect(within(confirmation).queryByText(/no hay stock suficiente/i)).not.toBeInTheDocument();
  });

  it("evita doble envío deshabilitando el botón de confirmación mientras la petición está en curso", async () => {
    seedSession("OPERATOR");
    let resolvePost!: (value: Response) => void;
    mockFetch(
      saleRoutes((url, init) => {
        if (url.includes("/sales") && init?.method === "POST") {
          return new Promise<Response>((resolve) => {
            resolvePost = resolve;
          });
        }
        return undefined;
      }),
    );
    renderApp("/ventas/nueva");

    await selectOption(/producto de la línea 1/i, "10");
    await userEvent.type(screen.getByLabelText(/cantidad de la línea 1/i), "1");
    await userEvent.click(screen.getByRole("button", { name: /revisar venta/i }));

    const dialog = await screen.findByRole("dialog", { name: /confirmar venta/i });
    const confirmButton = within(dialog).getByRole("button", { name: /confirmar venta/i });
    await userEvent.click(confirmButton);

    expect(await within(dialog).findByRole("button", { name: /confirmando/i })).toBeDisabled();

    resolvePost(jsonResponse(201, sale({ id: "1" })));
  });
});

describe("Comprobante de venta", () => {
  it("muestra líneas y totales", async () => {
    seedSession("OPERATOR");
    mockFetch(saleRoutes());
    renderApp("/ventas/700");

    expect(await screen.findByText("V-ABC12345")).toBeInTheDocument();
    const row = screen.getByText(/SKU-001 — Cemento gris/).closest("tr")!;
    expect(within(row).getByText("3")).toBeInTheDocument();
    expect(within(row).getByText("150")).toBeInTheDocument();
  });
});
