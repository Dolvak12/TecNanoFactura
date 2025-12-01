// =====================================================
// src/main/java/com/tecnano/factura/config/AppProperties.java
// =====================================================
package com.tecnano.factura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tecnano")
public class AppProperties {

    private final Negocio negocio = new Negocio();
    private final Impresion impresion = new Impresion();
    private final Sri sri = new Sri();
    private final Licencia licencia = new Licencia();

    public Negocio getNegocio() { return negocio; }
    public Impresion getImpresion() { return impresion; }
    public Sri getSri() { return sri; }
    public Licencia getLicencia() { return licencia; }

    // ======= Sección NEGOCIO =======
    public static class Negocio {
        private String nombre = "MI RESTAURANTE";
        private String ruc = "9999999999999";
        private String regimen = "RIMPE Negocio Popular (no genera IVA)";
        private String direccion = "Quito - Ecuador";
        private String telefono = "0980000000";

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getRuc() { return ruc; }
        public void setRuc(String ruc) { this.ruc = ruc; }

        public String getRegimen() { return regimen; }
        public void setRegimen(String regimen) { this.regimen = regimen; }

        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }

        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
    }

    // ======= Sección IMPRESIÓN =======
    public static class Impresion {
        private String impresoraCocina = "";

        public String getImpresoraCocina() { return impresoraCocina; }
        public void setImpresoraCocina(String impresoraCocina) { this.impresoraCocina = impresoraCocina; }
    }

    // ======= Sección SRI/API =======
    public static class Sri {
        private boolean simular = true;
        private String proveedorBaseUrl = "";
        private String proveedorToken = "";

        public boolean isSimular() { return simular; }
        public void setSimular(boolean simular) { this.simular = simular; }

        public String getProveedorBaseUrl() { return proveedorBaseUrl; }
        public void setProveedorBaseUrl(String proveedorBaseUrl) { this.proveedorBaseUrl = proveedorBaseUrl; }

        public String getProveedorToken() { return proveedorToken; }
        public void setProveedorToken(String proveedorToken) { this.proveedorToken = proveedorToken; }
    }

    // ======= Sección LICENCIA =======
    public static class Licencia {
        private String codigo = "TN-0000";

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
    }
}
