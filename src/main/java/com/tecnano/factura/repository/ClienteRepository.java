// =====================================================
// src/main/java/com/tecnano/factura/repository/ClienteRepository.java
// =====================================================
package com.tecnano.factura.repository;

import com.tecnano.factura.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroIdentificacion(String numeroIdentificacion);
}
