package com.tecnano.factura.web;

import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import com.tecnano.factura.service.SriService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints “genéricos” para trabajar con facturas/ventas vía HTTP.
 *
 * OJO:
 * - La UI principal es Vaadin (CajaView, VentasView, etc.).
 * - Este controller es útil para integraciones externas o pruebas
 *   (por ejemplo, desde Postman) sin tocar la UI.
 */
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final VentaRepository ventaRepository;
    private final SriService sriService;

    public FacturaController(VentaRepository ventaRepository,
                             SriService sriService) {
        this.ventaRepository = ventaRepository;
        this.sriService = sriService;
    }

    /**
     * Obtener una venta/factura por ID (JSON).
     *
     * GET /api/facturas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerVenta(@PathVariable("id") Long id) {
        return ventaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Listar ventas con estado SRI = PENDIENTE o ERROR.
     *
     * GET /api/facturas/pendientes-sri
     */
    @GetMapping("/pendientes-sri")
    public List<Venta> listarPendientesSri() {
        return ventaRepository.findAll().stream()
                .filter(v -> {
                    String est = normalizar(v.getEstadoSri());
                    return "PENDIENTE".equals(est) || "ERROR".equals(est);
                })
                .toList();
    }

    /**
     * Reintentar el envío de una venta al SRI.
     *
     * POST /api/facturas/{id}/enviar-sri
     *
     * Devuelve un mensaje corto con el estado final.
     */
    @PostMapping("/{id}/enviar-sri")
    public ResponseEntity<String> reenviarVentaAlSri(@PathVariable("id") Long id) {
        var opt = ventaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Venta no encontrada: " + id);
        }

        Venta venta = opt.get();
        try {
            // Procesamos con el servicio de SRI (genera XML, llama proveedor, etc.)
            sriService.procesarVentaConSri(venta);
            // Guardamos cualquier cambio de estado/claveAcceso/ridePdf, etc.
            ventaRepository.save(venta);

            String estado = normalizar(venta.getEstadoSri());
            String mensaje = switch (estado) {
                case "AUTORIZADA" ->
                        "Venta " + id + " AUTORIZADA por SRI.";
                case "ERROR" ->
                        "Venta " + id + " quedó en ERROR. Revisa mensajeErrorSri.";
                case "PENDIENTE" ->
                        "Venta " + id + " sigue en estado PENDIENTE.";
                default ->
                        "Venta " + id + " procesada. Estado actual: " + estado;
            };

            return ResponseEntity.ok(mensaje);

        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Desconocido";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar venta " + id + " al SRI: " + msg);
        }
    }

    // =========================
    // Helpers
    // =========================
    private String normalizar(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }
}
