package com.inventario.multisucursal.products;

import com.inventario.multisucursal.common.exception.ResourceConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * docs/API_DESIGN.md, sección 7.4. Catálogo global de unidades de medida —
 * sin actualización ni baja lógica en el contrato aprobado (ver
 * {@link UnitOfMeasure}), por eso el servicio solo cubre listar y crear.
 */
@Service
public class UnitOfMeasureService {

    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public UnitOfMeasureService(UnitOfMeasureRepository unitOfMeasureRepository) {
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    public List<UnitOfMeasureResponse> list() {
        return unitOfMeasureRepository.findAll().stream().map(UnitOfMeasureResponse::from).toList();
    }

    @Transactional
    public UnitOfMeasureResponse create(CreateUnitOfMeasureRequest request) {
        if (unitOfMeasureRepository.existsByCode(request.code())) {
            throw new ResourceConflictException("CODIGO_UNIDAD_YA_EXISTE", "Ya existe una unidad de medida con ese código.");
        }
        UnitOfMeasure unit = new UnitOfMeasure(request.code(), request.name());
        return UnitOfMeasureResponse.from(unitOfMeasureRepository.save(unit));
    }
}
