import { Modal } from "./Modal";

/**
 * Confirmación para acciones sensibles (desactivar un producto, registrar un
 * ajuste manual de stock). Muestra explícitamente lo que va a ocurrir en vez
 * de un "¿Estás seguro?" genérico.
 */
export function ConfirmDialog({
  title,
  message,
  confirmLabel = "Confirmar",
  isPending = false,
  onConfirm,
  onCancel,
}: {
  title: string;
  message: string;
  confirmLabel?: string;
  isPending?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p>{message}</p>
      <div className="modal__actions">
        <button type="button" onClick={onCancel} disabled={isPending}>
          Cancelar
        </button>
        <button type="button" className="button--danger" onClick={onConfirm} disabled={isPending}>
          {isPending ? "Procesando…" : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
