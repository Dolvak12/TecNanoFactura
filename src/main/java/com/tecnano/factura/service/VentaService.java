// =====================================================
// src/main/java/com/tecnano/factura/service/VentaService.java
// =====================================================
package com.tecnano.factura.service;

import com.tecnano.factura.model.Cliente;
import com.tecnano.factura.model.DetalleVenta;
import com.tecnano.factura.model.Producto;
import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.ProductoRepository;
import com.tecnano.factura.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final SriService sriService;

    public VentaService(VentaRepository ventaRepository,
                        ProductoRepository productoRepository,
                        SriService sriService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.sriService = sriService;
    }

    @Transactional
    public Venta registrarVentaDesdeLineas(List<Item> items,
                                           String formaPago,
                                           String ubicacion,
                                           Cliente cliente,
                                           String tipoCliente) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No hay ítems para registrar la venta.");
        }

        Venta venta = new Venta();
        venta.setFechaHora(LocalDateTime.now());
        venta.setFormaPago(formaPago != null ? formaPago : "Efectivo");
        venta.setUbicacion(ubicacion);
        venta.setTipoCliente(tipoCliente != null ? tipoCliente : "FINAL");

        if (cliente != null) {
            venta.setCliente(cliente);
        }

        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal baseIva12 = BigDecimal.ZERO;

        for (Item item : items) {
            if (item == null || item.productoId() == null) continue;

            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Producto no encontrado con id " + item.productoId()));

            DetalleVenta det = new DetalleVenta();
            det.setProducto(producto);

            int cantidad = item.cantidad() > 0 ? item.cantidad() : 1;
            det.setCantidad(cantidad);

            BigDecimal precioUnitario = item.precioUnitario() != null
                    ? item.precioUnitario()
                    : (producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO);
            det.setPrecioUnitario(precioUnitario);

            det.setNotaCocina(item.notaCocina());
            det.setVenta(venta);

            BigDecimal lineaSubtotal = precioUnitario
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
            det.setSubtotal(lineaSubtotal);

            subtotal = subtotal.add(lineaSubtotal);

            if (producto.isConIva()) {
                baseIva12 = baseIva12.add(lineaSubtotal);
                det.setConIva(true);
            } else {
                det.setConIva(false);
            }

            detalles.add(det);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = baseIva12
                .multiply(new BigDecimal("0.12"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

        venta.setSubtotal(subtotal);
        venta.setIva(iva);
        venta.setTotal(total);

        venta.setEstadoSri("PENDIENTE");
        venta.setDetalles(detalles);

        sriService.procesarVentaConSri(venta);

        return ventaRepository.save(venta);
    }

    @Transactional
    public Venta guardar(Venta venta) {
        return ventaRepository.save(venta);
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Venta> listarTodas() {
        return ventaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Venta> listarParaCocina() {
        Collection<String> estados = List.of("NUEVO", "EN_PREPARACION");
        return ventaRepository.findByEstadoCocinaInOrderByFechaHoraAsc(estados);
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPendientesSri() {
        return ventaRepository.findByEstadoSri("PENDIENTE");
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        return ventaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(desde, hasta);
    }

    public record Item(
            Long productoId,
            int cantidad,
            BigDecimal precioUnitario,
            String notaCocina
    ) {}
}
