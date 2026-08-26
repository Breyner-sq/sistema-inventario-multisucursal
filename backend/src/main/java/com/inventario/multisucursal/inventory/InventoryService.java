package com.inventario.multisucursal.inventory;

import com.inventario.multisucursal.common.exception.ResourceNotFoundException;
import com.inventario.multisucursal.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Consulta de stock (RF-002, RF-003, RF-006). Lectura abierta a cualquier
 * rol autenticado sobre cualquier sucursal (docs/API_DESIGN.md, sección 6) —
 * a diferencia del ajuste manual, aquí no aplica ninguna restricción de
 * sucursal propia.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public PageResponse<InventoryResponse> list(Long branchId, Long productId, String search, boolean lowStock, Pageable pageable) {
        return PageResponse.from(inventoryRepository.search(branchId, productId, search, lowStock, pageable).map(InventoryResponse::from));
    }

    public InventoryResponse getById(Long id) {
        return inventoryRepository.findById(id)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("INVENTARIO_NO_ENCONTRADO", "Registro de inventario no encontrado."));
    }
}
