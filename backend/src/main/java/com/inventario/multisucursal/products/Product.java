package com.inventario.multisucursal.products;

import com.inventario.multisucursal.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Producto (docs/DOMAIN_MODEL.md, sección 2.5; RF-005). Deliberadamente sin
 * ningún campo de cantidad/stock — eso pertenece a {@code Inventory}, un
 * módulo aparte que esta clase no anticipa ni acopla (condición de parada de
 * esta fase: "no implementes stock dentro de Product").
 *
 * <p>{@code sku} es la clave de negocio, inmutable después de creado —
 * igual que {@code Branch.code} (docs/API_DESIGN.md, sección 7.4: el PATCH
 * actualiza "nombre/descripción", no el SKU).
 */
@Entity
@Table(name = "product")
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "base_unit_of_measure_id", nullable = false)
    private Long baseUnitOfMeasureId;

    @Column(nullable = false)
    private boolean active = true;

    protected Product() {
        // JPA
    }

    public Product(String sku, String name, String description, Long baseUnitOfMeasureId) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.baseUnitOfMeasureId = baseUnitOfMeasureId;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getBaseUnitOfMeasureId() {
        return baseUnitOfMeasureId;
    }

    public boolean isActive() {
        return active;
    }
}
