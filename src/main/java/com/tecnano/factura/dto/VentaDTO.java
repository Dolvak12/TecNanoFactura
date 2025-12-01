// =====================================================
// src/main/java/com/tecnano/factura/dto/VentaDTO.java
// =====================================================
package com.tecnano.factura.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta al registrar una venta.
 */
public record VentaDTO(
        Long ventaId,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal total,
        BigDecimal efectivo,
        BigDecimal cambio
) {}
