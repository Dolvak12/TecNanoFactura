package com.tecnano.factura.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    /** 10 = cédula, 13 = RUC, otros */
    private String numeroIdentificacion;

    /** "04" RUC, "05" cédula, "06" pasaporte, "07" consumidor final */
    private String tipoIdentificacionSri;

    private String telefono;
    private String direccion;

    /** campo real en BD */
    private String email;

    /** Para compatibilidad con SriService (puede ser "FINAL" o "CON_DATOS") */
    private String tipoCliente = "FINAL";

    /* ========== Getters/Setters ========== */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }

    public String getTipoIdentificacionSri() { return tipoIdentificacionSri; }
    public void setTipoIdentificacionSri(String tipoIdentificacionSri) { this.tipoIdentificacionSri = tipoIdentificacionSri; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(String tipoCliente) { this.tipoCliente = tipoCliente; }

    /* ===== Alias para compatibilidad con CajaView ===== */
    public String getCorreo() { return email; }
    public void setCorreo(String correo) { this.email = correo; }
}
