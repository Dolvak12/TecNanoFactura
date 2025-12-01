// =====================================================
// src/main/java/com/tecnano/factura/model/Venta.java
// =====================================================
package com.tecnano.factura.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHora;

    /** "FINAL" o "CON_DATOS" */
    private String tipoCliente = "FINAL";

    /** "Efectivo", "Tarjeta", "Transferencia"... */
    private String formaPago;

    private String mesa;
    private String mesero;

    private String ubicacion;

    /** NUEVO | EN_PREPARACION | LISTA | ENTREGADA */
    private String estadoCocina;

    @Column(name = "estado_sri", length = 20)
    private String estadoSri;

    private String claveAccesoSri;
    private String numeroAutorizacionSri;

    @Column(length = 2000)
    private String mensajeErrorSri;

    @Lob
    @Column(name = "ride_pdf")
    private byte[] ridePdf;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Cliente cliente;

    @OneToMany(
            mappedBy = "venta",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DetalleVenta> detalles = new ArrayList<>();

    // Getters/Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(String tipoCliente) { this.tipoCliente = tipoCliente; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public String getMesa() { return mesa != null ? mesa : ubicacion; }
    public void setMesa(String mesa) { this.mesa = mesa; }

    public String getMesero() { return mesero; }
    public void setMesero(String mesero) { this.mesero = mesero; }

    public String getUbicacion() { return ubicacion != null ? ubicacion : mesa; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getEstadoCocina() { return estadoCocina; }
    public void setEstadoCocina(String estadoCocina) { this.estadoCocina = estadoCocina; }

    public String getEstadoSri() { return estadoSri; }
    public void setEstadoSri(String estadoSri) { this.estadoSri = estadoSri; }

    public String getClaveAccesoSri() { return claveAccesoSri; }
    public void setClaveAccesoSri(String claveAccesoSri) { this.claveAccesoSri = claveAccesoSri; }

    public String getNumeroAutorizacionSri() { return numeroAutorizacionSri; }
    public void setNumeroAutorizacionSri(String numeroAutorizacionSri) { this.numeroAutorizacionSri = numeroAutorizacionSri; }

    public String getMensajeErrorSri() { return mensajeErrorSri; }
    public void setMensajeErrorSri(String mensajeErrorSri) { this.mensajeErrorSri = mensajeErrorSri; }

    public byte[] getRidePdf() { return ridePdf; }
    public void setRidePdf(byte[] ridePdf) { this.ridePdf = ridePdf; }

    public BigDecimal getSubtotal() { return subtotal != null ? subtotal : BigDecimal.ZERO; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO; }

    public BigDecimal getIva() { return iva != null ? iva : BigDecimal.ZERO; }
    public void setIva(BigDecimal iva) { this.iva = iva != null ? iva : BigDecimal.ZERO; }

    public BigDecimal getTotal() { return total != null ? total : BigDecimal.ZERO; }
    public void setTotal(BigDecimal total) { this.total = total != null ? total : BigDecimal.ZERO; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles.clear();
        if (detalles != null) {
            detalles.forEach(this::addDetalle);
        }
    }

    public void addDetalle(DetalleVenta d) {
        if (d == null) return;
        d.setVenta(this);
        this.detalles.add(d);
    }
}
