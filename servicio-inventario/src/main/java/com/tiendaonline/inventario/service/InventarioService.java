package com.tiendaonline.inventario.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendaonline.inventario.entity.EstadoReserva;
import com.tiendaonline.inventario.entity.Producto;
import com.tiendaonline.inventario.entity.Reserva;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.exception.ReservaNoEncontradaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import com.tiendaonline.inventario.repository.ReservaRepository;

@Service
public class InventarioService {
    private final ProductoRepository productoRepository;
    private final ReservaRepository reservaRepository;

    public InventarioService(ProductoRepository productoRepository, ReservaRepository reservaRepository) {
        this.productoRepository = productoRepository;
        this.reservaRepository = reservaRepository;
    }

    public Producto obtenerProducto(Integer id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    @Transactional
    public Reserva reservarStock(Integer productoId, Integer pedidoId, Integer cantidad) {
        // El descuento y la verificacion de stock suceden en un unico UPDATE atomico
        int filasActualizadas = productoRepository.descontarStock(productoId, cantidad);

        if (filasActualizadas == 0) {
            // No se actualizo nada: o el producto no existe o no alcanza el stock
            if (!productoRepository.existsById(productoId)) {
                throw new ProductoNoEncontradoException(productoId);
            }
            throw new StockInsuficienteException();
        }

        Reserva reserva = new Reserva();
        reserva.setProductoId(productoId);
        reserva.setPedidoId(pedidoId);
        reserva.setCantidad(cantidad);
        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.setFechaCreacion(LocalDateTime.now());

        return reservaRepository.save(reserva);
    }

    // Liberar es idempotente: si la reserva ya estaba liberada no se repone stock de nuevo
    @Transactional
    public Reserva liberarReserva(Integer reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));

        if (reserva.getEstado() == EstadoReserva.LIBERADA) {
            return reserva;
        }

        liberar(reserva);
        return reserva;
    }

    // Libera todas las reservas activas de un pedido (lo usan la cancelacion y el batch de expiracion)
    @Transactional
    public List<Reserva> liberarReservasDePedido(Integer pedidoId) {
        List<Reserva> activas = reservaRepository.findByPedidoIdAndEstado(pedidoId, EstadoReserva.ACTIVA);

        for (Reserva reserva : activas) {
            liberar(reserva);
        }

        return activas;
    }

    private void liberar(Reserva reserva) {
        // Solo repone stock quien logra pasar la reserva de ACTIVA a LIBERADA.
        // Si dos liberaciones llegan a la vez, una sola ve filasCambiadas == 1.
        int filasCambiadas = reservaRepository.cambiarEstado(
            reserva.getId(), EstadoReserva.ACTIVA, EstadoReserva.LIBERADA);

        if (filasCambiadas == 1) {
            productoRepository.reponerStock(reserva.getProductoId(), reserva.getCantidad());
        }

        // Despues de este metodo la reserva esta LIBERADA en la base, sea quien sea que la liberó
        reserva.setEstado(EstadoReserva.LIBERADA);
    }
}
