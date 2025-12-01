// =====================================================
// src/main/java/com/tecnano/factura/api/VentaRestController.java
// =====================================================
package com.tecnano.factura.api;

import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/ventas")
public class VentaRestController {

    private final VentaRepository ventaRepository;

    public VentaRestController(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    /**
     * Devuelve el RIDE (PDF) de una venta.
     *
     * URL: GET /api/ventas/{id}/ride
     */
    @GetMapping("/{id}/ride")
    public ResponseEntity<byte[]> obtenerRide(@PathVariable("id") Long id) {
        Optional<Venta> optVenta = ventaRepository.findById(id);
        if (optVenta.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Venta venta = optVenta.get();

        byte[] pdfBytes = venta.getRidePdf();
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        ContentDisposition contentDisposition = ContentDisposition
                .inline()
                .filename("RIDE-venta-" + id + ".pdf")
                .build();
        headers.setContentDisposition(contentDisposition);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /*
    // Si en algún momento quieres exponer la venta como JSON:
    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerVenta(@PathVariable("id") Long id) {
        return ventaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    */
}
