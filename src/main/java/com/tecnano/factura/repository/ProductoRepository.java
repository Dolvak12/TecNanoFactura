// =====================================================
// src/main/java/com/tecnano/factura/repository/ProductoRepository.java
// =====================================================
package com.tecnano.factura.repository;

import com.tecnano.factura.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);
}
