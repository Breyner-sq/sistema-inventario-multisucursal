package com.inventario.multisucursal.products;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * docs/API_DESIGN.md, sección 7.4. Lectura abierta a cualquier rol
 * autenticado; a diferencia de {@code products} (escritura OPERATOR+ADMIN),
 * crear una unidad de medida nueva en el catálogo global es exclusivo de
 * ADMIN — el contrato lo especifica puntualmente para este endpoint, más
 * restrictivo que la fila general de la tabla de autorización.
 */
@RestController
@RequestMapping("/api/v1/units-of-measure")
public class UnitOfMeasureController {

    private final UnitOfMeasureService unitOfMeasureService;

    public UnitOfMeasureController(UnitOfMeasureService unitOfMeasureService) {
        this.unitOfMeasureService = unitOfMeasureService;
    }

    @GetMapping
    public List<UnitOfMeasureResponse> list() {
        return unitOfMeasureService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UnitOfMeasureResponse create(@Valid @RequestBody CreateUnitOfMeasureRequest request) {
        return unitOfMeasureService.create(request);
    }
}
