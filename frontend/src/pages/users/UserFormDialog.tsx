import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import type { FormEvent } from "react";
import { createUser } from "../../api/endpoints/users";
import { queryPrefixes } from "../../api/queryClient";
import { Field, FormErrorMessage, SelectField } from "../../components/form/Field";
import { toFormErrors } from "../../components/form/formErrors";
import { Modal } from "../../components/ui/Modal";
import type { Branch, Role, RoleInfo } from "../../types/api";

/**
 * Alta de usuario (UC-14): asigna un rol y, salvo para `ADMIN` (alcance
 * global, sin sucursal), la sucursal a la que queda restringido. El backend
 * vuelve a validar esa consistencia (`ADMIN_SIN_SUCURSAL`/`SUCURSAL_REQUERIDA`,
 * `UserService.validateRoleBranchConsistency`) — aquí solo se evita el envío
 * obviamente incompleto, nunca se decide la regla.
 */
export function UserFormDialog({ roles, branches, onClose }: { roles: RoleInfo[]; branches: Branch[]; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [branchId, setBranchId] = useState("");
  const [localErrors, setLocalErrors] = useState<Record<string, string>>({});

  const requiresBranch = role !== "" && role !== "ADMIN";

  const mutation = useMutation({
    mutationFn: () =>
      createUser({
        name: name.trim(),
        email: email.trim(),
        password,
        role: role as Role,
        branchId: requiresBranch ? Number(branchId) : null,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryPrefixes.users });
      onClose();
    },
  });

  const serverErrors = mutation.error ? toFormErrors(mutation.error) : { fields: {} as Record<string, string> };
  const errorFor = (field: string) => localErrors[field] ?? serverErrors.fields[field];

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    // Validación solo de forma; la semántica de negocio —correo repetido,
    // consistencia rol/sucursal— la decide el backend.
    const errors: Record<string, string> = {};
    if (!name.trim()) errors.name = "El nombre es obligatorio.";
    if (!email.trim()) errors.email = "El correo es obligatorio.";
    if (password.length < 8) errors.password = "La contraseña debe tener al menos 8 caracteres.";
    if (!role) errors.role = "Selecciona un rol.";
    if (requiresBranch && !branchId) errors.branchId = "Selecciona la sucursal.";
    setLocalErrors(errors);
    if (Object.keys(errors).length > 0) return;
    mutation.mutate();
  }

  return (
    <Modal title="Nuevo usuario" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <Field id="user-name" label="Nombre" value={name} maxLength={150} onChange={(event) => setName(event.target.value)} error={errorFor("name")} />

        <Field
          id="user-email"
          label="Correo electrónico"
          type="email"
          value={email}
          maxLength={255}
          onChange={(event) => setEmail(event.target.value)}
          error={errorFor("email")}
        />

        <Field
          id="user-password"
          label="Contraseña"
          type="password"
          value={password}
          maxLength={100}
          onChange={(event) => setPassword(event.target.value)}
          error={errorFor("password")}
        />

        <SelectField
          id="user-role"
          label="Rol"
          value={role}
          onChange={(event) => {
            setRole(event.target.value as Role | "");
            setBranchId("");
          }}
          error={errorFor("role")}
        >
          <option value="">Selecciona…</option>
          {roles.map((option) => (
            <option key={option.code} value={option.code}>
              {option.name}
            </option>
          ))}
        </SelectField>

        {requiresBranch ? (
          <SelectField id="user-branch" label="Sucursal" value={branchId} onChange={(event) => setBranchId(event.target.value)} error={errorFor("branchId")}>
            <option value="">Selecciona…</option>
            {branches.map((branch) => (
              <option key={branch.id} value={branch.id}>
                {branch.name}
              </option>
            ))}
          </SelectField>
        ) : role === "ADMIN" ? (
          <p className="state__hint">Un administrador general tiene alcance global, sin sucursal asignada.</p>
        ) : null}

        <FormErrorMessage error={serverErrors.general ? mutation.error : null} />

        <div className="modal__actions">
          <button type="button" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </button>
          <button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando…" : "Guardar"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
