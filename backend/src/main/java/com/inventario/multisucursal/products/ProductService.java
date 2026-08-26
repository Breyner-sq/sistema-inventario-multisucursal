package com.inventario.multisucursal.products;

import com.inventario.multisucursal.common.exception.ResourceConflictException;
import com.inventario.multisucursal.common.exception.ResourceNotFoundException;
import com.inventario.multisucursal.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * RF-005 (docs/PROJECT_BRIEF.md, sección 3.2): CRUD de productos. Toda
 * escritura está restringida a OPERATOR/ADMIN a nivel de
 * {@link ProductController} (docs/API_DESIGN.md, sección 6) — este servicio
 * no vuelve a comprobar el rol, solo las reglas propias del recurso.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final ProductUnitRepository productUnitRepository;

    public ProductService(
            ProductRepository productRepository,
            UnitOfMeasureRepository unitOfMeasureRepository,
            ProductUnitRepository productUnitRepository) {
        this.productRepository = productRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.productUnitRepository = productUnitRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ResourceConflictException("SKU_YA_EXISTE", "Ya existe un producto con ese SKU.");
        }
        UnitOfMeasure baseUnit = unitOfMeasureRepository.findById(request.baseUnitOfMeasureId())
                .orElseThrow(() -> new ResourceNotFoundException("UNIDAD_DE_MEDIDA_NO_ENCONTRADA", "Unidad de medida no encontrada."));

        Product product = productRepository.save(
                new Product(request.sku(), request.name(), request.description(), baseUnit.getId()));

        // BR-011 / docs/DOMAIN_MODEL.md, sección 2.6: la unidad base se crea
        // automáticamente con factor 1 - garantiza que siempre exista una
        // única unidad base inequívoca sin depender de una segunda llamada
        // manual a POST /products/{id}/units.
        productUnitRepository.save(new ProductUnit(product.getId(), baseUnit.getId(), BigDecimal.ONE, true));

        return ProductResponse.from(product);
    }

    public ProductResponse getById(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    public PageResponse<ProductResponse> list(String search, Boolean active, Pageable pageable) {
        return PageResponse.from(productRepository.search(search, active, pageable).map(ProductResponse::from));
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findOrThrow(id);
        product.updateDetails(request.name(), request.description());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse activate(Long id) {
        Product product = findOrThrow(id);
        product.activate();
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse deactivate(Long id) {
        Product product = findOrThrow(id);
        product.deactivate();
        return ProductResponse.from(product);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado."));
    }
}
