package com.inventario.multisucursal.transfers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Cada transición de estado es un {@code UPDATE ... WHERE status = <esperado>}:
 * una comparación y escritura atómica que la base de datos serializa. Si
 * devuelve 0 filas, otra solicitud ya aplicó la transición (o el estado
 * nunca fue el esperado) y el servicio responde 409 — así se resuelven, sin
 * bloqueos ni claves de idempotencia, el despacho duplicado, la recepción
 * duplicada y la doble aprobación (docs/CRITICAL_FLOWS.md, sección 1.1,
 * categoría 1).
 *
 * <p>Las transiciones que además fijan datos del hito (aprobador, fecha,
 * transportista) lo hacen <b>en la misma sentencia</b>, no con una mutación
 * posterior de la entidad: si el guard falla, esos datos tampoco se escriben.
 */
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByClientReferenceId(String clientReferenceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Transfer t
               SET t.status = com.inventario.multisucursal.transfers.TransferStatus.APPROVED,
                   t.approvedByUserId = :userId, t.approvedAt = :now
             WHERE t.id = :id AND t.status = com.inventario.multisucursal.transfers.TransferStatus.REQUESTED
            """)
    int markApproved(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Transfer t
               SET t.status = com.inventario.multisucursal.transfers.TransferStatus.REJECTED,
                   t.approvedByUserId = :userId, t.approvedAt = :now
             WHERE t.id = :id AND t.status = com.inventario.multisucursal.transfers.TransferStatus.REQUESTED
            """)
    int markRejected(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Transfer t
               SET t.status = com.inventario.multisucursal.transfers.TransferStatus.IN_TRANSIT,
                   t.carrierName = :carrierName, t.estimatedArrivalDate = :estimatedArrivalDate, t.dispatchedAt = :now
             WHERE t.id = :id AND t.status = com.inventario.multisucursal.transfers.TransferStatus.APPROVED
            """)
    int markDispatched(
            @Param("id") Long id,
            @Param("carrierName") String carrierName,
            @Param("estimatedArrivalDate") LocalDate estimatedArrivalDate,
            @Param("now") Instant now);

    /** Cierre de la recepción: solo cuando todas las líneas quedaron atendidas (docs/API_DESIGN.md, sección 7.9). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Transfer t
               SET t.status = :newStatus, t.receivedAt = :now
             WHERE t.id = :id AND t.status = com.inventario.multisucursal.transfers.TransferStatus.IN_TRANSIT
            """)
    int markReceived(@Param("id") Long id, @Param("newStatus") TransferStatus newStatus, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Transfer t
               SET t.status = com.inventario.multisucursal.transfers.TransferStatus.CLOSED
             WHERE t.id = :id AND t.status = com.inventario.multisucursal.transfers.TransferStatus.RECEIVED_PARTIAL
            """)
    int markClosed(@Param("id") Long id);

    /**
     * Lectura: una transferencia es visible desde su sucursal origen o su
     * destino (docs/API_DESIGN.md, sección 6). {@code branchId} nulo (solo
     * ADMIN llega así) no filtra por sucursal.
     */
    @Query("""
            SELECT t FROM Transfer t
             WHERE (:branchId IS NULL
                        OR (:role IS NULL AND (t.originBranchId = :branchId OR t.destinationBranchId = :branchId))
                        OR (:role = 'origin' AND t.originBranchId = :branchId)
                        OR (:role = 'destination' AND t.destinationBranchId = :branchId))
               AND (:status IS NULL OR t.status = :status)
            """)
    Page<Transfer> search(
            @Param("branchId") Long branchId,
            @Param("role") String role,
            @Param("status") TransferStatus status,
            Pageable pageable);
}
