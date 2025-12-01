// =====================================================
// src/main/java/com/tecnano/factura/service/ProveedorSriApiClient.java
// =====================================================
package com.tecnano.factura.service;

import com.tecnano.factura.config.AppProperties;
import com.tecnano.factura.model.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProveedorSriApiClient {

    private static final Logger log = LoggerFactory.getLogger(ProveedorSriApiClient.class);

    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    public ProveedorSriApiClient(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restTemplate = new RestTemplate();
    }

    public ResultadoEnvio enviarFactura(Venta venta, byte[] xmlFirmado, byte[] pdfRide) {
        boolean simular = appProperties.getSri().isSimular();
        if (simular) {
            log.info("Simulando envío a SRI para venta {} (tecnano.sri.simular = true)", venta.getId());
            return simularEnvio(venta, xmlFirmado, pdfRide);
        }

        String baseUrl = appProperties.getSri().getProveedorBaseUrl();
        String token = appProperties.getSri().getProveedorToken();

        if (baseUrl == null || baseUrl.isBlank()) {
            String msg = "No se ha configurado tecnano.sri.proveedor-base-url";
            log.error(msg);
            return ResultadoEnvio.error(msg);
        }

        if (token == null || token.isBlank()) {
            String msg = "No se ha configurado tecnano.sri.proveedor-token";
            log.error(msg);
            return ResultadoEnvio.error(msg);
        }

        try {
            String url = baseUrl.endsWith("/")
                    ? baseUrl + "facturas"
                    : baseUrl + "/facturas";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, Object> body = construirBodyGenerico(venta, xmlFirmado, pdfRide);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            log.info("Enviando factura {} al proveedor SRI en {}", venta.getId(), url);

            ResponseEntity<ProveedorResponse> response =
                    restTemplate.postForEntity(url, request, ProveedorResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                String msg = "Respuesta no exitosa del proveedor SRI: " + response.getStatusCode();
                log.error(msg);
                return ResultadoEnvio.error(msg);
            }

            ProveedorResponse pr = response.getBody();
            log.info("Respuesta del proveedor SRI para venta {}: estado={} mensaje={}",
                    venta.getId(), pr.estado(), pr.mensaje());

            String estadoSri = pr.estado();
            String mensajeError = null;
            if (!"AUTORIZADA".equalsIgnoreCase(estadoSri)) {
                mensajeError = pr.mensaje();
            }

            byte[] rideBytes = null;
            if (pr.ridePdfBase64() != null && !pr.ridePdfBase64().isBlank()) {
                rideBytes = Base64.getDecoder().decode(pr.ridePdfBase64());
            }

            return new ResultadoEnvio(
                    estadoSri,
                    mensajeError,
                    pr.claveAcceso(),
                    pr.numeroAutorizacion(),
                    rideBytes
            );

        } catch (Exception ex) {
            String msg = "Error llamando al proveedor SRI: " + ex.getMessage();
            log.error("Error llamando al proveedor SRI para venta {}: {}", venta.getId(), ex.getMessage(), ex);
            return ResultadoEnvio.error(msg);
        }
    }

    private ResultadoEnvio simularEnvio(Venta venta, byte[] xmlFirmado, byte[] pdfRide) {
        String claveAccesoFake = "SIM-" + (venta.getId() != null ? venta.getId() : "0");
        String numeroAutoFake = "AUTO-" + UUID.randomUUID();

        return new ResultadoEnvio(
                "AUTORIZADA",
                null,
                claveAccesoFake,
                numeroAutoFake,
                pdfRide
        );
    }

    private Map<String, Object> construirBodyGenerico(Venta venta, byte[] xmlFirmado, byte[] pdfRide) {
        Map<String, Object> body = new HashMap<>();

        String xmlBase64 = xmlFirmado != null
                ? Base64.getEncoder().encodeToString(xmlFirmado)
                : null;
        body.put("xmlFirmadoBase64", xmlBase64);

        if (pdfRide != null && pdfRide.length > 0) {
            body.put("ridePdfBase64", Base64.getEncoder().encodeToString(pdfRide));
        }

        body.put("numeroInterno", venta.getId());
        body.put("formaPago", venta.getFormaPago());
        body.put("tipoCliente", venta.getTipoCliente());
        body.put("ubicacion", venta.getUbicacion());

        if (venta.getFechaHora() != null) {
            String fecha = venta.getFechaHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            body.put("fechaEmision", fecha);
        }

        body.put("rucEmisor", appProperties.getNegocio().getRuc());
        body.put("nombreComercial", appProperties.getNegocio().getNombre());

        return body;
    }

    // DTO interno

    public static class ResultadoEnvio {
        private final String estadoSri;
        private final String mensajeError;
        private final String claveAccesoSri;
        private final String numeroAutorizacionSri;
        private final byte[] ridePdf;

        public ResultadoEnvio(String estadoSri,
                              String mensajeError,
                              String claveAccesoSri,
                              String numeroAutorizacionSri,
                              byte[] ridePdf) {
            this.estadoSri = estadoSri;
            this.mensajeError = mensajeError;
            this.claveAccesoSri = claveAccesoSri;
            this.numeroAutorizacionSri = numeroAutorizacionSri;
            this.ridePdf = ridePdf;
        }

        public static ResultadoEnvio error(String mensaje) {
            return new ResultadoEnvio("ERROR", mensaje, null, null, null);
        }

        public String getEstadoSri() { return estadoSri; }
        public String getMensajeError() { return mensajeError; }
        public String getClaveAccesoSri() { return claveAccesoSri; }
        public String getNumeroAutorizacionSri() { return numeroAutorizacionSri; }
        public byte[] getRidePdf() { return ridePdf; }
    }

    public record ProveedorResponse(
            String estado,
            String mensaje,
            String claveAcceso,
            String numeroAutorizacion,
            String ridePdfBase64
    ) {}
}
