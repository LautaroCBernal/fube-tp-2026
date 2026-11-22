package com.tiendaonline.pedidos.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiendaonline.pedidos.client.InventarioClient;
import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.Pedido;
import com.tiendaonline.pedidos.exception.RespuestaInvalidaException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;

@ExtendWith(MockitoExtension.class)
public class ExpirarPedidoProcessorTest {
    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private ExpirarPedidoProcessor processor;

    private Pedido crearPedido(Integer id) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setEstado(EstadoPedido.CREADO);
        return pedido;
    }

    @Test
    void expiraElPedidoYLiberaSuStock() {
        Pedido pedido = crearPedido(7);

        Pedido resultado = processor.process(pedido);

        verify(inventarioClient).liberarReservasDePedido(7);
        assertSame(pedido, resultado);
        assertEquals(EstadoPedido.EXPIRADO, resultado.getEstado());
    }

    @Test
    void siInventarioNoEstaDisponibleElPedidoSeDescartaYSigueCreado() {
        Pedido pedido = crearPedido(7);
        doThrow(new ServicioNoDisponibleException("inventario")).when(inventarioClient).liberarReservasDePedido(7);

        Pedido resultado = processor.process(pedido);

        // null = Spring Batch no lo guarda: se reintenta en la proxima corrida
        assertNull(resultado);
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
    }

    @Test
    void siInventarioDevuelveUnaRespuestaInvalidaElPedidoSeDescartaYSigueCreado() {
        Pedido pedido = crearPedido(7);
        doThrow(new RespuestaInvalidaException("inventario")).when(inventarioClient).liberarReservasDePedido(7);

        assertNull(processor.process(pedido));
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
    }
}
