package com.tiendaonline.pedidos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.pedidos.dto.ItemPedidoRequest;
import com.tiendaonline.pedidos.dto.ItemPedidoResponse;
import com.tiendaonline.pedidos.dto.PedidoRequest;
import com.tiendaonline.pedidos.dto.PedidoResponse;
import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.exception.ClienteNoEncontradoException;
import com.tiendaonline.pedidos.exception.EstadoPedidoInvalidoException;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;
import com.tiendaonline.pedidos.service.PedidoService;

// Carga solo el contexto de Spring necesario
@WebMvcTest(PedidoController.class)
public class PedidoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService service;

    // Arma un pedido de ejemplo con un item
    private PedidoResponse crearPedidoResponse() {
        ItemPedidoResponse item = new ItemPedidoResponse(1, 2, 3, new BigDecimal("100.00"));

        return new PedidoResponse(1, 5, EstadoPedido.CREADO, new BigDecimal("300.00"),
            LocalDateTime.of(2026, 1, 1, 0, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0, 0), List.of(item));
    }

    // ---------- GET /v1/pedidos/{id} ----------

    @Test
    void obtenerPedidoExistente() throws Exception {
        when(service.obtenerPedido(1)).thenReturn(crearPedidoResponse());

        mockMvc.perform(get("/v1/pedidos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.clienteId").value(5))
            .andExpect(jsonPath("$.estado").value("CREADO"))
            .andExpect(jsonPath("$.montoTotal").value(300.00))
            .andExpect(jsonPath("$.fechaCreacion").value("2026-01-01T00:00:00"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].productoId").value(2))
            .andExpect(jsonPath("$.items[0].cantidad").value(3))
            .andExpect(jsonPath("$.items[0].precioUnitario").value(100.00));

        verify(service).obtenerPedido(1);
    }

    @Test
    void obtenerPedidoInexistente() throws Exception {
        when(service.obtenerPedido(99)).thenThrow(new PedidoNoEncontradoException(99));

        mockMvc.perform(get("/v1/pedidos/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.mensaje").value("No existe el pedido con id 99"));
    }

    @Test
    void obtenerPedidoConIdNoNumerico() throws Exception {
        mockMvc.perform(get("/v1/pedidos/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value("El parametro 'id' tiene un valor invalido"));

        verifyNoInteractions(service);
    }

    // ---------- GET /v1/pedidos?clienteId= ----------

    @Test
    void listarPedidosDeUnCliente() throws Exception {
        when(service.listarPedidosPorCliente(5)).thenReturn(List.of(crearPedidoResponse()));

        mockMvc.perform(get("/v1/pedidos").param("clienteId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].clienteId").value(5))
            .andExpect(jsonPath("$[0].items[0].productoId").value(2));

        verify(service).listarPedidosPorCliente(5);
    }

    @Test
    void listarPedidosDeUnClienteSinPedidos() throws Exception {
        when(service.listarPedidosPorCliente(5)).thenReturn(List.of());

        mockMvc.perform(get("/v1/pedidos").param("clienteId", "5"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void listarPedidosSinClienteId() throws Exception {
        mockMvc.perform(get("/v1/pedidos"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value("Falta el parametro obligatorio: clienteId"));

        verifyNoInteractions(service);
    }

    // ---------- POST /v1/pedidos ----------

    private static final String BODY_VALIDO = "{\"clienteId\":5,\"items\":[{\"productoId\":2,\"cantidad\":3}]}";

    @Test
    void crearPedidoExitoso() throws Exception {
        when(service.crearPedido(any(PedidoRequest.class))).thenReturn(crearPedidoResponse());

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.estado").value("CREADO"))
            .andExpect(jsonPath("$.montoTotal").value(300.00));

        // Se verifica que el JSON se convirtio bien al request que recibe el service
        verify(service).crearPedido(new PedidoRequest(5, List.of(new ItemPedidoRequest(2, 3))));
    }

    @Test
    void crearPedidoSinItems() throws Exception {
        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":5,\"items\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value(containsString("items")));

        verifyNoInteractions(service);
    }

    @Test
    void crearPedidoConCantidadInvalida() throws Exception {
        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":5,\"items\":[{\"productoId\":2,\"cantidad\":0}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("cantidad")));

        verifyNoInteractions(service);
    }

    @Test
    void crearPedidoSinClienteId() throws Exception {
        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productoId\":2,\"cantidad\":3}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("clienteId")));

        verifyNoInteractions(service);
    }

    @Test
    void crearPedidoConJsonMalFormado() throws Exception {
        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El cuerpo de la peticion es invalido"));

        verifyNoInteractions(service);
    }

    @Test
    void crearPedidoConClienteInexistente() throws Exception {
        when(service.crearPedido(any(PedidoRequest.class))).thenThrow(new ClienteNoEncontradoException(5));

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.mensaje").value("No existe el cliente con id 5"));
    }

    @Test
    void crearPedidoSinStockSuficiente() throws Exception {
        when(service.crearPedido(any(PedidoRequest.class))).thenThrow(new StockInsuficienteException());

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void crearPedidoConInventarioNoDisponible() throws Exception {
        when(service.crearPedido(any(PedidoRequest.class))).thenThrow(new ServicioNoDisponibleException("inventario"));

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.mensaje").value("El servicio de inventario no esta disponible en este momento"));
    }

    // ---------- POST /v1/pedidos/{id}/cancelar ----------

    // Copia de la respuesta de ejemplo pero con otro estado
    private PedidoResponse conEstado(PedidoResponse base, EstadoPedido estado) {
        return new PedidoResponse(base.id(), base.clienteId(), estado, base.montoTotal(),
            base.fechaCreacion(), base.fechaActualizacion(), base.items());
    }

    @Test
    void cancelarPedidoExistente() throws Exception {
        when(service.cancelarPedido(1)).thenReturn(conEstado(crearPedidoResponse(), EstadoPedido.CANCELADO));

        mockMvc.perform(post("/v1/pedidos/1/cancelar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.estado").value("CANCELADO"));

        verify(service).cancelarPedido(1);
    }

    @Test
    void cancelarPedidoInexistente() throws Exception {
        when(service.cancelarPedido(99)).thenThrow(new PedidoNoEncontradoException(99));

        mockMvc.perform(post("/v1/pedidos/99/cancelar"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.mensaje").value("No existe el pedido con id 99"));
    }

    @Test
    void cancelarPedidoEnEstadoInvalido() throws Exception {
        when(service.cancelarPedido(1))
            .thenThrow(new EstadoPedidoInvalidoException(1, EstadoPedido.CANCELADO, "cancelar"));

        mockMvc.perform(post("/v1/pedidos/1/cancelar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.mensaje").value("No se puede cancelar el pedido 1 porque esta en estado CANCELADO"));
    }

    @Test
    void cancelarPedidoConInventarioNoDisponible() throws Exception {
        when(service.cancelarPedido(1)).thenThrow(new ServicioNoDisponibleException("inventario"));

        mockMvc.perform(post("/v1/pedidos/1/cancelar"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503));
    }

    // ---------- POST /v1/pedidos/{id}/confirmar ----------

    @Test
    void confirmarPedidoExistente() throws Exception {
        when(service.confirmarPedido(1)).thenReturn(conEstado(crearPedidoResponse(), EstadoPedido.CONFIRMADO));

        mockMvc.perform(post("/v1/pedidos/1/confirmar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        verify(service).confirmarPedido(1);
    }

    @Test
    void confirmarPedidoInexistente() throws Exception {
        when(service.confirmarPedido(99)).thenThrow(new PedidoNoEncontradoException(99));

        mockMvc.perform(post("/v1/pedidos/99/confirmar"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void confirmarPedidoEnEstadoInvalido() throws Exception {
        when(service.confirmarPedido(1))
            .thenThrow(new EstadoPedidoInvalidoException(1, EstadoPedido.CONFIRMADO, "confirmar"));

        mockMvc.perform(post("/v1/pedidos/1/confirmar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.mensaje").value("No se puede confirmar el pedido 1 porque esta en estado CONFIRMADO"));
    }
}
