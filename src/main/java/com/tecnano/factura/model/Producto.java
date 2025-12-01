// =====================================================
// src/main/java/com/tecnano/factura/model/Producto.java
// =====================================================
package com.tecnano.factura.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column
    private Integer stock;

    @Column(nullable = false)
    private boolean conIva;

    public Producto() {}

    public Producto(String codigo, String nombre, BigDecimal precio, Integer stock, boolean conIva) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.conIva = conIva;
    }

    // Getters y setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public boolean isConIva() { return conIva; }
    public void setConIva(boolean conIva) { this.conIva = conIva; }

    // Alias

    public BigDecimal getPrecioVenta() { return getPrecio(); }
    public void setPrecioVenta(BigDecimal v) { setPrecio(v); }
}
