// =====================================================
// src/main/java/com/tecnano/factura/model/DetalleVenta.java
// =====================================================
package com.tecnano.factura.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "detalles_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int cantidad = 1;

    @Column(precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    private boolean conIva = false;

    @Column(length = 1000)
    private String notaCocina;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    private Venta venta;

    // ========== Getters/Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }

    public String getNotaCocina() { return notaCocina; }
    public void setNotaCocina(String notaCocina) { this.notaCocina = notaCocina; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public boolean isConIva() { return conIva; }
    public void setConIva(boolean conIva) { this.conIva = conIva; }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = (subtotal != null) ? subtotal.setScale(2, RoundingMode.HALF_UP) : null;
    }

    // ========== Calculado ==========

    @Transient
    public BigDecimal getSubtotal() {
        if (subtotal != null) return subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pu = getPrecioUnitario();
        return pu.multiply(BigDecimal.valueOf(Math.max(1, cantidad)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
