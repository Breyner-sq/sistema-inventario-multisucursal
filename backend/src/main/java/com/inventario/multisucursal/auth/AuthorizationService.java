package com.inventario.multisucursal.auth;

import com.inventario.multisucursal.common.exception.BranchAccessDeniedException;
import com.inventario.multisucursal.users.RoleCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Autorización por sucursal, para usar en {@code @PreAuthorize} junto con
 * (o en vez de) {@code hasRole(...)} — BR-018 exige que ciertas acciones se
 * limiten tanto por rol como por la sucursal del usuario, no solo por rol.
 *
 * <p>{@code ADMIN} tiene alcance global (docs/DOMAIN_MODEL.md, sección 2.3) y
 * siempre pasa esta comprobación. No hay ningún módulo de negocio todavía que
 * la consuma en producción — se verifica end-to-end con un controlador de
 * prueba (ver {@code AuthorizationDemoController} en el código de test),
 * quedando lista para que los módulos futuros la reutilicen.
 */
@Component("authz")
public class AuthorizationService {

    public boolean requireBranchAccess(Long branchId) {
        AuthenticatedUser user = currentUser();
        if (user.role() == RoleCode.ADMIN) {
            return true;
        }
        if (branchId != null && branchId.equals(user.branchId())) {
            return true;
        }
        throw new BranchAccessDeniedException(
                "El usuario no pertenece a la sucursal solicitada.");
    }

    /**
     * Acceso cuando un recurso involucra a más de una sucursal y basta con
     * pertenecer a cualquiera de ellas — el caso de {@code Transfer}, visible
     * tanto desde su origen como desde su destino (docs/API_DESIGN.md,
     * sección 6). {@code ADMIN} conserva su alcance global.
     */
    public boolean requireAnyBranchAccess(Long... branchIds) {
        AuthenticatedUser user = currentUser();
        if (user.role() == RoleCode.ADMIN) {
            return true;
        }
        for (Long branchId : branchIds) {
            if (branchId != null && branchId.equals(user.branchId())) {
                return true;
            }
        }
        throw new BranchAccessDeniedException(
                "El usuario no pertenece a ninguna de las sucursales involucradas.");
    }

    /** La sucursal del usuario autenticado, o {@code null} si es {@code ADMIN} (alcance global). */
    public Long currentBranchScopeOrNull() {
        AuthenticatedUser user = currentUser();
        return user.role() == RoleCode.ADMIN ? null : user.branchId();
    }

    private AuthenticatedUser currentUser() {
        return (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
