package com.tiendaonline.pedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiendaonline.pedidos.client.ClienteClient;
import com.tiendaonline.pedidos.client.InventarioClient;
import com.tiendaonline.pedidos.dto.ItemPedidoRequest;
import com.tiendaonline.pedidos.dto.PedidoRequest;
import com.tiendaonline.pedidos.dto.PedidoResponse;
import com.tiendaonline.pedidos.dto.ReservaResponse;
import com.tiendaonline.pedidos.dto.StockResponse;
import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.ItemPedido;
import com.tiendaonline.pedidos.entity.Pedido;
import com.tiendaonline.pedidos.exception.ClienteNoEncontradoException;
import com.tiendaonline.pedidos.exception.EstadoPedidoInvalidoException;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ProductoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;
import com.tiendaonline.pedidos.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteClient clienteClient;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private PedidoService service;

    // Arma un pedido de ejemplo (cliente 5) con un item de producto 2, cantidad 3, a $100
    private Pedido crearPedido(Integer id) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setClienteId(5);
        pedido.setEstado(EstadoPedido.CREADO);
        pedido.setMontoTotal(new BigDecimal("300.00"));
        pedido.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 0, 0));
        pedido.setFechaActualizacion(LocalDateTime.of(2026, 1, 1, 0, 0));

        ItemPedido item = new ItemPedido();
        item.setId(10);
        item.setProductoId(2);
        item.setCantidad(3);
        item.setPrecioUnitario(new BigDecimal("100.00"));
        pedido.agregarItem(item);

        return pedido;
    }

    @Test
    void obtenerPedidoExistenteDevuelveElPedidoConSusItems() {
        when(pedidoRepository.findById(1)).thenReturn(Optional.of(crearPedido(1)));

        PedidoResponse respuesta = service.obtenerPedido(1);

        assertEquals(1, respuesta.id());
        assertEquals(5, respuesta.clienteId());
        assertEquals(EstadoPedido.CREADO, respuesta.estado());
        assertEquals(new BigDecimal("300.00"), respuesta.montoTotal());
        assertEquals(1, respuesta.items().size());
        assertEquals(2, respuesta.items().get(0).productoId());
        assertEquals(3, respuesta.items().get(0).cantidad());
        assertEquals(new BigDecimal("100.00"), respuesta.items().get(0).precioUnitario());
    }

    @Test
    void obtenerPedidoInexistenteLanzaExcepcion() {
        when(pedidoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(PedidoNoEncontradoException.class, () -> service.obtenerPedido(99));
    }

    @Test
    void listarPedidosDeUnClienteLosDevuelveTodos() {
        when(pedidoRepository.findByClienteIdOrderByFechaCreacionDesc(5))
            .thenReturn(List.of(crearPedido(1), crearPedido(2)));

        List<PedidoResponse> respuesta = service.listarPedidosPorCliente(5);

        assertEquals(2, respuesta.size());
        assertEquals(1, respuesta.get(0).id());
        assertEquals(2, respuesta.get(1).id());
        verify(pedidoRepository).findByClienteIdOrderByFechaCreacionDesc(5);
    }

    @Test
    void listarPedidosDeUnClienteSinPedidosDevuelveListaVacia() {
        when(pedidoRepository.findByClienteIdOrderByFechaCreacionDesc(5)).thenReturn(List.of());

        assertTrue(service.listarPedidosPorCliente(5).isEmpty());
    }

    // ---------- crearPedido ----------

    // Pedido de cliente 5 con dos items: producto 2 (x3, $500) y producto 3 (x1, $200) -> total $1700
    private PedidoRequest crearRequest() {
        return new PedidoRequest(5, List.of(new ItemPedidoRequest(2, 3), new ItemPedidoRequest(3, 1)));
    }

    // Simula que la base le asigna el id 7 al pedido al guardarlo
    private void simularGuardadoConId7() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            pedido.setId(7);
            return pedido;
        });
    }

    private void simularStockDeLosDosProductos() {
        when(inventarioClient.obtenerStock(2)).thenReturn(new StockResponse(2, "Teclado", new BigDecimal("500.00"), 10));
        when(inventarioClient.obtenerStock(3)).thenReturn(new StockResponse(3, "Mouse", new BigDecimal("200.00"), 10));
    }

    // La primera reserva sale bien y la segunda falla por falta de stock. Hay que configurar las dos:
    // con Mockito estricto, llamar a un metodo con otros argumentos que los configurados falla el test.
    private void simularPrimeraReservaOkYSegundaSinStock() {
        when(inventarioClient.reservar(2, 7, 3)).thenReturn(new ReservaResponse(1, 2, 7, 3, "ACTIVA", null));
        doThrow(new StockInsuficienteException()).when(inventarioClient).reservar(3, 7, 1);
    }

    @Test
    void crearPedidoExitosoCalculaElMontoYReservaCadaItem() {
        when(clienteClient.existeCliente(5)).thenReturn(true);
        simularStockDeLosDosProductos();
        simularGuardadoConId7();

        PedidoResponse respuesta = service.crearPedido(crearRequest());

        assertEquals(7, respuesta.id());
        assertEquals(5, respuesta.clienteId());
        assertEquals(EstadoPedido.CREADO, respuesta.estado());
        // 3 x 500 + 1 x 200
        assertEquals(0, new BigDecimal("1700.00").compareTo(respuesta.montoTotal()));
        assertEquals(2, respuesta.items().size());
        // El precio se toma de inventario, no del request
        assertEquals(0, new BigDecimal("500.00").compareTo(respuesta.items().get(0).precioUnitario()));

        verify(inventarioClient).reservar(2, 7, 3);
        verify(inventarioClient).reservar(3, 7, 1);
        verify(inventarioClient, never()).liberarReservasDePedido(any());
        verify(pedidoRepository, never()).deleteById(any());
    }

    @Test
    void crearPedidoConClienteInexistenteNoTocaInventarioNiGuardaNada() {
        when(clienteClient.existeCliente(5)).thenReturn(false);

        assertThrows(ClienteNoEncontradoException.class, () -> service.crearPedido(crearRequest()));

        verifyNoInteractions(inventarioClient);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearPedidoConProductoInexistenteNoGuardaNada() {
        when(clienteClient.existeCliente(5)).thenReturn(true);
        when(inventarioClient.obtenerStock(2)).thenThrow(new ProductoNoEncontradoException(2));

        assertThrows(ProductoNoEncontradoException.class, () -> service.crearPedido(crearRequest()));

        verify(pedidoRepository, never()).save(any());
        verify(inventarioClient, never()).reservar(any(), any(), any());
    }

    @Test
    void crearPedidoSinStockEnElSegundoItemLiberaLoReservadoYBorraElPedido() {
        when(clienteClient.existeCliente(5)).thenReturn(true);
        simularStockDeLosDosProductos();
        simularGuardadoConId7();
        simularPrimeraReservaOkYSegundaSinStock();

        assertThrows(StockInsuficienteException.class, () -> service.crearPedido(crearRequest()));

        verify(inventarioClient).reservar(2, 7, 3);
        verify(inventarioClient).liberarReservasDePedido(7);
        verify(pedidoRepository).deleteById(7);
    }

    @Test
    void crearPedidoSiFallaLaCompensacionElPedidoQuedaCancelado() {
        when(clienteClient.existeCliente(5)).thenReturn(true);
        simularStockDeLosDosProductos();
        simularGuardadoConId7();
        simularPrimeraReservaOkYSegundaSinStock();
        // Inventario tampoco responde cuando se intenta liberar
        doThrow(new ServicioNoDisponibleException("inventario")).when(inventarioClient).liberarReservasDePedido(7);

        // El error que le llega al cliente sigue siendo el original (falta de stock)
        assertThrows(StockInsuficienteException.class, () -> service.crearPedido(crearRequest()));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, times(2)).save(captor.capture());
        assertEquals(EstadoPedido.CANCELADO, captor.getValue().getEstado());
        verify(pedidoRepository, never()).deleteById(any());
    }

    // ---------- cancelarPedido ----------

    @Test
    void cancelarPedidoCreadoLiberaElStockYLoDejaCancelado() {
        when(pedidoRepository.findConItemsById(1)).thenReturn(Optional.of(crearPedido(1)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponse respuesta = service.cancelarPedido(1);

        verify(inventarioClient).liberarReservasDePedido(1);
        assertEquals(EstadoPedido.CANCELADO, respuesta.estado());
        assertEquals(1, respuesta.items().size());
    }

    @Test
    void cancelarPedidoConfirmadoTambienSePuedeCancelar() {
        Pedido pedido = crearPedido(1);
        pedido.setEstado(EstadoPedido.CONFIRMADO);

        when(pedidoRepository.findConItemsById(1)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponse respuesta = service.cancelarPedido(1);

        verify(inventarioClient).liberarReservasDePedido(1);
        assertEquals(EstadoPedido.CANCELADO, respuesta.estado());
    }

    @Test
    void cancelarPedidoCanceladoOExpiradoLanzaExcepcionYNoTocaInventario() {
        Pedido pedido = crearPedido(1);
        when(pedidoRepository.findConItemsById(1)).thenReturn(Optional.of(pedido));

        // Se prueba con los dos estados que no admiten cancelacion
        for (EstadoPedido estado : List.of(EstadoPedido.CANCELADO, EstadoPedido.EXPIRADO)) {
            pedido.setEstado(estado);

            assertThrows(EstadoPedidoInvalidoException.class, () -> service.cancelarPedido(1));
        }

        verifyNoInteractions(inventarioClient);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelarPedidoInexistenteLanzaExcepcion() {
        when(pedidoRepository.findConItemsById(99)).thenReturn(Optional.empty());

        assertThrows(PedidoNoEncontradoException.class, () -> service.cancelarPedido(99));

        verifyNoInteractions(inventarioClient);
    }

    @Test
    void cancelarPedidoConInventarioCaidoNoCambiaElEstado() {
        Pedido pedido = crearPedido(1);

        when(pedidoRepository.findConItemsById(1)).thenReturn(Optional.of(pedido));
        doThrow(new ServicioNoDisponibleException("inventario")).when(inventarioClient).liberarReservasDePedido(1);

        assertThrows(ServicioNoDisponibleException.class, () -> service.cancelarPedido(1));

        // El pedido sigue CREADO y no se guardo nada: se puede reintentar la cancelacion
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        verify(pedidoRepository, never()).save(any());
    }

    // ---------- confirmarPedido ----------

    @Test
    void confirmarPedidoCreadoLoDejaConfirmado() {
        when(pedidoRepository.findById(1)).thenReturn(Optional.of(crearPedido(1)));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponse respuesta = service.confirmarPedido(1);

        assertEquals(EstadoPedido.CONFIRMADO, respuesta.estado());
        verifyNoInteractions(inventarioClient);
    }

    @Test
    void confirmarPedidoQueNoEstaCreadoLanzaExcepcion() {
        Pedido pedido = crearPedido(1);
        pedido.setEstado(EstadoPedido.CONFIRMADO);

        when(pedidoRepository.findById(1)).thenReturn(Optional.of(pedido));

        assertThrows(EstadoPedidoInvalidoException.class, () -> service.confirmarPedido(1));

        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    void confirmarPedidoInexistenteLanzaExcepcion() {
        when(pedidoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(PedidoNoEncontradoException.class, () -> service.confirmarPedido(99));
    }
}
