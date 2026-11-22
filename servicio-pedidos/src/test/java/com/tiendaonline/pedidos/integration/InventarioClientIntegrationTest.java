package com.tiendaonline.pedidos.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tiendaonline.pedidos.client.InventarioClient;
import com.tiendaonline.pedidos.dto.ReservaResponse;
import com.tiendaonline.pedidos.dto.StockResponse;
import com.tiendaonline.pedidos.exception.ProductoNoEncontradoException;
import com.tiendaonline.pedidos.exception.RespuestaInvalidaException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;

// MockServer hace de servicio-inventario
class InventarioClientIntegrationTest {
    private ClientAndServer mockServer;
    private InventarioClient inventarioClient;

    @BeforeEach
    void setUp() {
        mockServer = ClientAndServer.startClientAndServer(0);
        String url = "http://localhost:" + mockServer.getLocalPort();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        RestClient restClient = RestClient.builder()
            .baseUrl(url)
            .requestFactory(factory)
            .build();

        inventarioClient = new InventarioClient(restClient);
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    // ---------- obtenerStock ----------

    @Test
    void obtenerStockExitoso() {
        mockServer.when(request().withMethod("GET").withPath("/v1/productos/2/stock"))
            .respond(response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"productoId\":2,\"nombre\":\"Teclado\",\"precio\":500.00,\"cantidadDisponible\":10}"));

        StockResponse stock = inventarioClient.obtenerStock(2);

        assertEquals(2, stock.productoId());
        assertEquals("Teclado", stock.nombre());
        assertEquals(0, new BigDecimal("500.00").compareTo(stock.precio()));
        assertEquals(10, stock.cantidadDisponible());
    }

    @Test
    void obtenerStockProductoInexistente() {
        mockServer.when(request().withMethod("GET").withPath("/v1/productos/99/stock"))
            .respond(response().withStatusCode(404));

        assertThrows(ProductoNoEncontradoException.class, () -> inventarioClient.obtenerStock(99));
    }

    // ---------- reservar ----------

    @Test
    void reservarExitoso() {
        // Ademas de la respuesta, se verifica que el cuerpo que manda pedidos sea el que inventario espera
        mockServer.when(request()
                .withMethod("POST")
                .withPath("/v1/reservas")
                .withBody(json("{\"productoId\":2,\"pedidoId\":7,\"cantidad\":3}")))
            .respond(response()
                .withStatusCode(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":1,\"productoId\":2,\"pedidoId\":7,\"cantidad\":3,\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-19T10:00:00\"}"));

        ReservaResponse reserva = inventarioClient.reservar(2, 7, 3);

        assertEquals(1, reserva.id());
        assertEquals(7, reserva.pedidoId());
        assertEquals("ACTIVA", reserva.estado());
    }

    @Test
    void reservarSinStockSuficiente() {
        mockServer.when(request().withMethod("POST").withPath("/v1/reservas"))
            .respond(response()
                .withStatusCode(409)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":409,\"mensaje\":\"No hay stock suficiente para realizar la reserva\"}"));

        assertThrows(StockInsuficienteException.class, () -> inventarioClient.reservar(2, 7, 300));
    }

    @Test
    void reservarProductoInexistente() {
        mockServer.when(request().withMethod("POST").withPath("/v1/reservas"))
            .respond(response().withStatusCode(404));

        assertThrows(ProductoNoEncontradoException.class, () -> inventarioClient.reservar(99, 7, 1));
    }

    @Test
    void reservarConRespuestaInvalida() {
        mockServer.when(request().withMethod("POST").withPath("/v1/reservas"))
            .respond(response().withStatusCode(500));

        assertThrows(RespuestaInvalidaException.class, () -> inventarioClient.reservar(2, 7, 1));
    }

    // ---------- liberarReservasDePedido ----------

    @Test
    void liberarReservasDePedidoExitoso() {
        mockServer.when(request()
                .withMethod("POST")
                .withPath("/v1/reservas/liberar")
                .withQueryStringParameter("pedidoId", "7"))
            .respond(response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]"));

        assertDoesNotThrow(() -> inventarioClient.liberarReservasDePedido(7));
    }

    @Test
    void liberarReservasConServicioNoDisponible() {
        mockServer.stop();

        assertThrows(ServicioNoDisponibleException.class, () -> inventarioClient.liberarReservasDePedido(7));
    }
}
