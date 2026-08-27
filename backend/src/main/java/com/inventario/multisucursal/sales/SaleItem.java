package com.inventario.multisucursal.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Línea de venta (docs/DOMAIN_MODEL.md, sección 2.16; RF-017, RF-020).
 * Inmutable una vez creada — {@code unitPrice} es el precio efectivamente
 * cobrado (copiado de {@code Price} vigente al confirmar), no cambia si el
 * precio de referencia cambia después (BR-021).
 */
@Entity
@Table(name = "sale_item", uniqueConstraints = @UniqueConstraint(columnNames = {"sale_id", "product_id"}))
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "unit_of_measure_id", nullable = false)
    private Long unitOfMeasureId;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    protected SaleItem() {
        // JPA
    }

    public SaleItem(
            Long saleId, Long productId, Long unitOfMeasureId, BigDecimal quantity,
            BigDecimal unitPrice, BigDecimal discountPercentage, BigDecimal lineTotal) {
        this.saleId = saleId;
        this.productId = productId;
        this.unitOfMeasureId = unitOfMeasureId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountPercentage = discountPercentage;
        this.lineTotal = lineTotal;
    }

    public Long getId() {
        return id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
