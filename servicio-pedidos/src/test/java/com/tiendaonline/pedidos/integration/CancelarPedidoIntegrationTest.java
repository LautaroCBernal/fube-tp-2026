package com.tiendaonline.pedidos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.ItemPedido;
import com.tiendaonline.pedidos.entity.Pedido;
import com.tiendaonline.pedidos.repository.PedidoRepository;

// Prueba cancelar y confirmar pedidos contra la base real. Lo unico falso es servicio-inventario (MockServer).
// Los pedidos se insertan directo en la base, sin pasar por POST /v1/pedidos.
// Requiere PostgreSQL levantado con pedidos_db.
@SpringBootTest
@AutoConfigureMockMvc
class CancelarPedidoIntegrationTest {

    private static final ClientAndServer inventarioServer = ClientAndServer.startClientAndServer(0);

    @DynamicPropertySource
    static void urlDeInventario(DynamicPropertyRegistry registry) {
        registry.add("servicios.inventario.url", () -> "http://localhost:" + inventarioServer.getLocalPort());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        inventarioServer.reset();
        pedidoRepository.deleteAll();
    }

    @AfterEach
    void limpiar() {
        pedidoRepository.deleteAll();
    }

    @AfterAll
    static void apagarServidor() {
        inventarioServer.stop();
    }

    // Guarda en la base un pedido con un item y devuelve su id
    private Integer guardarPedido(EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(5);
        pedido.setEstado(estado);
        pedido.setMontoTotal(new BigDecimal("1500.00"));

        ItemPedido item = new ItemPedido();
        item.setProductoId(2);
        item.setCantidad(3);
        item.setPrecioUnitario(new BigDecimal("500.00"));
        pedido.agregarItem(item);

        return pedidoRepository.save(pedido).getId();
    }

    private EstadoPedido estadoEnLaBase(Integer id) {
        return pedidoRepository.findById(id).orElseThrow().getEstado();
    }

    private void inventarioLiberaOk() {
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas/liberar"))
            .respond(response().withStatusCode(200).withHeader("Content-Type", "application/json").withBody("[]"));
    }

    @Test
    void cancelarPedidoLiberaElStockYLoDejaCancelado() throws Exception {
        Integer id = guardarPedido(EstadoPedido.CREADO);
        inventarioLiberaOk();

        mockMvc.perform(post("/v1/pedidos/" + id + "/cancelar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"))
            // Los items sobreviven al guardado y se pueden leer en la respuesta
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].productoId").value(2));

        assertEquals(EstadoPedido.CANCELADO, estadoEnLaBase(id));
        inventarioServer.verify(request().withMethod("POST").withPath("/v1/reservas/liberar"), VerificationTimes.once());
    }

    @Test
    void cancelarPedidoConInventarioCaidoDevuelve503YDejaElPedidoComoEstaba() throws Exception {
        Integer id = guardarPedido(EstadoPedido.CREADO);
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas/liberar"))
            .error(error().withDropConnection(true));

        mockMvc.perform(post("/v1/pedidos/" + id + "/cancelar"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503));

        // Como no se pudo liberar el stock, el pedido NO se cancela: se puede reintentar
        assertEquals(EstadoPedido.CREADO, estadoEnLaBase(id));
    }

    @Test
    void cancelarPedidoYaCanceladoDevuelve409SinLlamarAInventario() throws Exception {
        Integer id = guardarPedido(EstadoPedido.CANCELADO);

        mockMvc.perform(post("/v1/pedidos/" + id + "/cancelar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        inventarioServer.verifyZeroInteractions();
    }

    @Test
    void cancelarPedidoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/v1/pedidos/999999/cancelar"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void confirmarPedidoLoDejaConfirmado() throws Exception {
        Integer id = guardarPedido(EstadoPedido.CREADO);

        mockMvc.perform(post("/v1/pedidos/" + id + "/confirmar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.items.length()").value(1));

        assertEquals(EstadoPedido.CONFIRMADO, estadoEnLaBase(id));
        // Confirmar no le habla a inventario
        inventarioServer.verifyZeroInteractions();
    }

    @Test
    void confirmarPedidoQueNoEstaCreadoDevuelve409() throws Exception {
        Integer id = guardarPedido(EstadoPedido.CANCELADO);

        mockMvc.perform(post("/v1/pedidos/" + id + "/confirmar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        assertEquals(EstadoPedido.CANCELADO, estadoEnLaBase(id));
    }
}
