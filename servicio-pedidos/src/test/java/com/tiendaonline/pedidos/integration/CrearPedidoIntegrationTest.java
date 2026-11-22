package com.tiendaonline.pedidos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.pedidos.repository.PedidoRepository;

// Prueba el flujo completo de crear un pedido: controller -> service -> clientes HTTP -> base de datos real.
// Lo unico falso son los otros dos servicios, que se simulan con MockServer.
// Requiere PostgreSQL levantado con pedidos_db (igual que ServicioPedidosApplicationTests).
@SpringBootTest
@AutoConfigureMockMvc
class CrearPedidoIntegrationTest {

    // Se levantan antes de que arranque el contexto de Spring, en puertos libres
    private static final ClientAndServer clientesServer = ClientAndServer.startClientAndServer(0);
    private static final ClientAndServer inventarioServer = ClientAndServer.startClientAndServer(0);

    // Le indica a Spring que use los MockServer como URL de "servicio-clientes" y "servicio-inventario"
    @DynamicPropertySource
    static void urlsDeServicios(DynamicPropertyRegistry registry) {
        registry.add("servicios.clientes.url", () -> "http://localhost:" + clientesServer.getLocalPort());
        registry.add("servicios.inventario.url", () -> "http://localhost:" + inventarioServer.getLocalPort());
    }

    private static final String BODY = "{\"clienteId\":5,\"items\":[{\"productoId\":2,\"cantidad\":3}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        clientesServer.reset();
        inventarioServer.reset();
        pedidoRepository.deleteAll();
    }

    @AfterEach
    void limpiar() {
        pedidoRepository.deleteAll();
    }

    @AfterAll
    static void apagarServidores() {
        clientesServer.stop();
        inventarioServer.stop();
    }

    private void clienteExiste() {
        clientesServer.when(request().withMethod("GET").withPath("/v1/clientes/5"))
            .respond(response().withStatusCode(200).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":5,\"nombre\":\"J*** P***\",\"email\":\"****@****.com\",\"fechaAlta\":\"2026-11-19T10:00:00\"}"));
    }

    private void productoConPrecio500() {
        inventarioServer.when(request().withMethod("GET").withPath("/v1/productos/2/stock"))
            .respond(response().withStatusCode(200).withHeader("Content-Type", "application/json")
                .withBody("{\"productoId\":2,\"nombre\":\"Teclado\",\"precio\":500.00,\"cantidadDisponible\":10}"));
    }

    @Test
    void crearPedidoExitosoLoGuardaYReservaElStock() throws Exception {
        clienteExiste();
        productoConPrecio500();
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas"))
            .respond(response().withStatusCode(201).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":1,\"productoId\":2,\"pedidoId\":1,\"cantidad\":3,\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-19T10:00:00\"}"));

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("CREADO"))
            .andExpect(jsonPath("$.montoTotal").value(1500.00))
            .andExpect(jsonPath("$.items[0].precioUnitario").value(500.00));

        // El pedido quedo guardado en la base
        assertEquals(1, pedidoRepository.count());
        // Y se reservo exactamente una vez
        inventarioServer.verify(request().withMethod("POST").withPath("/v1/reservas"), VerificationTimes.once());
    }

    @Test
    void crearPedidoConClienteInexistenteNoGuardaNiReserva() throws Exception {
        clientesServer.when(request().withMethod("GET").withPath("/v1/clientes/5"))
            .respond(response().withStatusCode(404));

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("No existe el cliente con id 5"));

        assertEquals(0, pedidoRepository.count());
        // Inventario no recibio ninguna llamada
        inventarioServer.verifyZeroInteractions();
    }

    @Test
    void crearPedidoSinStockLiberaLasReservasYNoDejaElPedido() throws Exception {
        clienteExiste();
        productoConPrecio500();
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas"))
            .respond(response().withStatusCode(409));
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas/liberar"))
            .respond(response().withStatusCode(200).withHeader("Content-Type", "application/json").withBody("[]"));

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        // La compensacion: se pidio liberar y el pedido se borro
        inventarioServer.verify(request().withMethod("POST").withPath("/v1/reservas/liberar"), VerificationTimes.once());
        assertEquals(0, pedidoRepository.count());
    }

    @Test
    void crearPedidoConInventarioCaidoDevuelve503() throws Exception {
        clienteExiste();
        // Inventario corta la conexion sin responder: para pedidos es lo mismo que un servicio caido
        inventarioServer.when(request().withMethod("GET").withPath("/v1/productos/2/stock"))
            .error(error().withDropConnection(true));

        mockMvc.perform(post("/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503));

        assertEquals(0, pedidoRepository.count());
    }
}
