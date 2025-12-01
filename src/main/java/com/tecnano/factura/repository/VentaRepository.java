// =====================================================
// src/main/java/com/tecnano/factura/repository/VentaRepository.java
// =====================================================
package com.tecnano.factura.repository;

import com.tecnano.factura.model.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @EntityGraph(attributePaths = {
            "detalles",
            "detalles.producto",
            "cliente"
    })
    List<Venta> findByEstadoCocinaInOrderByFechaHoraAsc(Collection<String> estados);

    List<Venta> findByEstadoSri(String estadoSri);

    List<Venta> findByFechaHoraBetweenOrderByFechaHoraAsc(LocalDateTime desde, LocalDateTime hasta);

    @EntityGraph(attributePaths = {
            "detalles",
            "detalles.producto",
            "cliente"
    })
    List<Venta> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    @Override
    @EntityGraph(attributePaths = {
            "detalles",
            "detalles.producto",
            "cliente"
    })
    Optional<Venta> findById(Long id);
}
