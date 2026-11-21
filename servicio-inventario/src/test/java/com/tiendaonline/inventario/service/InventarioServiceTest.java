package com.tiendaonline.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiendaonline.inventario.entity.EstadoReserva;
import com.tiendaonline.inventario.entity.Producto;
import com.tiendaonline.inventario.entity.Reserva;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.exception.ReservaNoEncontradaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import com.tiendaonline.inventario.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
public class InventarioServiceTest {
    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private InventarioService service;

    // Arma una reserva con los datos minimos para los tests de liberacion
    private Reserva crearReserva(Integer id, Integer productoId, Integer cantidad, EstadoReserva estado) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setProductoId(productoId);
        reserva.setPedidoId(10);
        reserva.setCantidad(cantidad);
        reserva.setEstado(estado);
        return reserva;
    }

    // ---------- obtenerProducto ----------

    @Test
    void obtenerProductoExistente() {
        Producto producto = new Producto();
        producto.setId(1);
        producto.setNombre("producto");

        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));

        Producto encontrado = service.obtenerProducto(1);

        assertSame(producto, encontrado);
        verify(productoRepository, times(1)).findById(1);
    }

    @Test
    void obtenerProductoInexistente() {
        when(productoRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(
            ProductoNoEncontradoException.class,
            () -> service.obtenerProducto(1)
        );
    }

    // ---------- reservarStock ----------

    @Test
    void reservarStockExitoso() {
        // El UPDATE condicional afecta una fila: habia stock suficiente
        when(productoRepository.descontarStock(1, 3)).thenReturn(1);
        when(reservaRepository.save(any(Reserva.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = service.reservarStock(1, 10, 3);

        verify(productoRepository).descontarStock(1, 3);
        verify(reservaRepository).save(any(Reserva.class));

        assertEquals(1, reserva.getProductoId());
        assertEquals(10, reserva.getPedidoId());
        assertEquals(3, reserva.getCantidad());
        assertEquals(EstadoReserva.ACTIVA, reserva.getEstado());
        assertNotNull(reserva.getFechaCreacion());
    }

    @Test
    void reservarStockInsuficiente() {
        // 0 filas actualizadas pero el producto existe: el problema es el stock
        when(productoRepository.descontarStock(1, 50)).thenReturn(0);
        when(productoRepository.existsById(1)).thenReturn(true);

        assertThrows(
            StockInsuficienteException.class,
            () -> service.reservarStock(1, 10, 50)
        );

        // No se debe haber guardado ninguna reserva
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservarStockProductoInexistente() {
        // 0 filas actualizadas y el producto no existe
        when(productoRepository.descontarStock(99, 1)).thenReturn(0);
        when(productoRepository.existsById(99)).thenReturn(false);

        assertThrows(
            ProductoNoEncontradoException.class,
            () -> service.reservarStock(99, 10, 1)
        );

        verify(reservaRepository, never()).save(any());
    }

    // ---------- liberarReserva ----------

    @Test
    void liberarReservaActiva() {
        Reserva reserva = crearReserva(5, 1, 3, EstadoReserva.ACTIVA);

        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(reservaRepository.cambiarEstado(5, EstadoReserva.ACTIVA, EstadoReserva.LIBERADA)).thenReturn(1);

        Reserva liberada = service.liberarReserva(5);

        // Se repone exactamente la cantidad reservada en el producto correcto
        verify(productoRepository).reponerStock(1, 3);
        assertEquals(EstadoReserva.LIBERADA, liberada.getEstado());
    }

    @Test
    void liberarReservaYaLiberadaNoRepoNiTocaNada() {
        Reserva reserva = crearReserva(5, 1, 3, EstadoReserva.LIBERADA);

        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));

        Reserva resultado = service.liberarReserva(5);

        assertEquals(EstadoReserva.LIBERADA, resultado.getEstado());
        // Idempotente: no cambia estado ni repone stock
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any());
        verifyNoInteractions(productoRepository);
    }

    @Test
    void liberarReservaCuandoOtroLaLiberoAntesNoRepone() {
        // La reserva se leyo como ACTIVA, pero otra liberacion concurrente se adelanto:
        // el UPDATE condicional no cambia ninguna fila
        Reserva reserva = crearReserva(5, 1, 3, EstadoReserva.ACTIVA);

        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(reservaRepository.cambiarEstado(5, EstadoReserva.ACTIVA, EstadoReserva.LIBERADA)).thenReturn(0);

        Reserva resultado = service.liberarReserva(5);

        assertEquals(EstadoReserva.LIBERADA, resultado.getEstado());
        // Clave: no se repone stock dos veces
        verify(productoRepository, never()).reponerStock(any(), any());
    }

    @Test
    void liberarReservaInexistente() {
        when(reservaRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(
            ReservaNoEncontradaException.class,
            () -> service.liberarReserva(5)
        );

        verifyNoInteractions(productoRepository);
    }

    // ---------- liberarReservasDePedido ----------

    @Test
    void liberarReservasDePedidoLiberaTodasLasActivas() {
        Reserva r1 = crearReserva(1, 100, 2, EstadoReserva.ACTIVA);
        Reserva r2 = crearReserva(2, 200, 5, EstadoReserva.ACTIVA);

        when(reservaRepository.findByPedidoIdAndEstado(10, EstadoReserva.ACTIVA)).thenReturn(List.of(r1, r2));
        when(reservaRepository.cambiarEstado(1, EstadoReserva.ACTIVA, EstadoReserva.LIBERADA)).thenReturn(1);
        when(reservaRepository.cambiarEstado(2, EstadoReserva.ACTIVA, EstadoReserva.LIBERADA)).thenReturn(1);

        List<Reserva> liberadas = service.liberarReservasDePedido(10);

        assertEquals(2, liberadas.size());
        verify(productoRepository).reponerStock(100, 2);
        verify(productoRepository).reponerStock(200, 5);
        assertTrue(liberadas.stream().allMatch(r -> r.getEstado() == EstadoReserva.LIBERADA));
    }

    @Test
    void liberarReservasDePedidoSinReservasActivas() {
        when(reservaRepository.findByPedidoIdAndEstado(10, EstadoReserva.ACTIVA)).thenReturn(List.of());

        List<Reserva> liberadas = service.liberarReservasDePedido(10);

        assertTrue(liberadas.isEmpty());
        verifyNoInteractions(productoRepository);
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any());
    }
}
