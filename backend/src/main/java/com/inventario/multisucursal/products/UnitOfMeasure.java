package com.inventario.multisucursal.products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catálogo de unidades de medida (docs/DOMAIN_MODEL.md, sección 2.4; RF-011).
 * Sin columnas de auditoría ni de estado: el contrato aprobado
 * (docs/API_DESIGN.md, sección 7.4) solo define {@code GET} y {@code POST}
 * para este recurso — no hay actualización ni baja lógica que auditar.
 */
@Entity
@Table(name = "unit_of_measure")
public class UnitOfMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    protected UnitOfMeasure() {
        // JPA
    }

    public UnitOfMeasure(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
