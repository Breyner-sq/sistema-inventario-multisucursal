package com.inventario.multisucursal.products;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    Optional<Price> findByPriceListIdAndProductIdAndValidToIsNull(Long priceListId, Long productId);

    List<Price> findByPriceListIdAndValidToIsNull(Long priceListId);

    List<Price> findByPriceListId(Long priceListId);
}
