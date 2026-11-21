package com.tiendaonline.inventario.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.inventario.entity.EstadoReserva;
import com.tiendaonline.inventario.entity.Reserva;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.exception.ReservaNoEncontradaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.service.InventarioService;

@WebMvcTest(ReservaController.class)
public class ReservaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventarioService service;

    private Reserva crearReserva(EstadoReserva estado) {
        Reserva reserva = new Reserva();
        reserva.setId(5);
        reserva.setProductoId(1);
        reserva.setPedidoId(10);
        reserva.setCantidad(3);
        reserva.setEstado(estado);
        reserva.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        return reserva;
    }

    // ---------- POST /v1/reservas ----------

    @Test
    void reservarValido() throws Exception {
        when(service.reservarStock(1, 10, 3)).thenReturn(crearReserva(EstadoReserva.ACTIVA));

        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "productoId": 1,
                    "pedidoId": 10,
                    "cantidad": 3
                }
            """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.productoId").value(1))
        .andExpect(jsonPath("$.pedidoId").value(10))
        .andExpect(jsonPath("$.cantidad").value(3))
        .andExpect(jsonPath("$.estado").value("ACTIVA"))
        .andExpect(jsonPath("$.fechaCreacion").value("2026-01-01T00:00:00"));

        verify(service).reservarStock(1, 10, 3);
    }

    @Test
    void reservarStockInsuficiente() throws Exception {
        when(service.reservarStock(1, 10, 50)).thenThrow(new StockInsuficienteException());

        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "productoId": 1,
                    "pedidoId": 10,
                    "cantidad": 50
                }
            """)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.mensaje").value("No hay stock suficiente para realizar la reserva"));
    }

    @Test
    void reservarProductoInexistente() throws Exception {
        when(service.reservarStock(99, 10, 1)).thenThrow(new ProductoNoEncontradoException(99));

        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "productoId": 99,
                    "pedidoId": 10,
                    "cantidad": 1
                }
            """)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.mensaje").value("No existe el producto con id 99"));
    }

    @Test
    void reservarCantidadCero() throws Exception {
        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "productoId": 1,
                    "pedidoId": 10,
                    "cantidad": 0
                }
            """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.mensaje").value("cantidad: Debe ser mayor a 0. "));

        verifyNoInteractions(service);
    }

    @Test
    void reservarSinProductoId() throws Exception {
        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "pedidoId": 10,
                    "cantidad": 2
                }
            """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.mensaje").value("productoId: No puede ser nulo. "));

        verifyNoInteractions(service);
    }

    @Test
    void reservarBodyMalFormado() throws Exception {
        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ esto no es json")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.mensaje").value("El cuerpo de la peticion es invalido"));

        verifyNoInteractions(service);
    }

    // ---------- POST /v1/reservas/{id}/liberar ----------

    @Test
    void liberarReservaExistente() throws Exception {
        when(service.liberarReserva(5)).thenReturn(crearReserva(EstadoReserva.LIBERADA));

        mockMvc.perform(post("/v1/reservas/5/liberar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.estado").value("LIBERADA"));

        verify(service).liberarReserva(5);
    }

    @Test
    void liberarReservaInexistente() throws Exception {
        when(service.liberarReserva(99)).thenThrow(new ReservaNoEncontradaException(99));

        mockMvc.perform(post("/v1/reservas/99/liberar"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.mensaje").value("No existe la reserva con id 99"));
    }

    // ---------- POST /v1/reservas/liberar?pedidoId= ----------

    @Test
    void liberarReservasDePedido() throws Exception {
        when(service.liberarReservasDePedido(10)).thenReturn(List.of(crearReserva(EstadoReserva.LIBERADA)));

        mockMvc.perform(post("/v1/reservas/liberar").param("pedidoId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].estado").value("LIBERADA"));

        verify(service).liberarReservasDePedido(10);
    }

    @Test
    void liberarReservasDePedidoSinReservas() throws Exception {
        when(service.liberarReservasDePedido(10)).thenReturn(List.of());

        mockMvc.perform(post("/v1/reservas/liberar").param("pedidoId", "10"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
    @Test
    void liberarReservasDePedidoSinPedidoId() throws Exception {
        mockMvc.perform(post("/v1/reservas/liberar"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value("Falta el parametro obligatorio: pedidoId"));

        verifyNoInteractions(service);
    }

    @Test
    void liberarReservasDePedidoConPedidoIdNoNumerico() throws Exception {
        mockMvc.perform(post("/v1/reservas/liberar").param("pedidoId", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value("El parametro 'pedidoId' tiene un valor invalido"));

        verifyNoInteractions(service);
    }
}
