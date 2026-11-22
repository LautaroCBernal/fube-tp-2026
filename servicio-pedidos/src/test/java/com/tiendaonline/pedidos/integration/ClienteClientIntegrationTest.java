package com.tiendaonline.pedidos.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tiendaonline.pedidos.client.ClienteClient;
import com.tiendaonline.pedidos.exception.RespuestaInvalidaException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;

// MockServer hace de servicio-clientes: se levanta un servidor HTTP real en un puerto libre
// y se le configura que responder, asi se prueba el cliente HTTP de verdad (sin mockear RestClient).
class ClienteClientIntegrationTest {
    private ClientAndServer mockServer;
    private ClienteClient clienteClient;

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

        clienteClient = new ClienteClient(restClient);
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    @Test
    void clienteExistente() {
        mockServer.when(request().withMethod("GET").withPath("/v1/clientes/1"))
            .respond(response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":1,\"nombre\":\"J*** P***\",\"email\":\"****@****.com\",\"fechaAlta\":\"2026-11-19T10:00:00\"}"));

        assertTrue(clienteClient.existeCliente(1));
    }

    @Test
    void clienteInexistente() {
        mockServer.when(request().withMethod("GET").withPath("/v1/clientes/99"))
            .respond(response().withStatusCode(404));

        assertFalse(clienteClient.existeCliente(99));
    }

    @Test
    void servicioClientesRespondeError500() {
        mockServer.when(request().withMethod("GET").withPath("/v1/clientes/1"))
            .respond(response().withStatusCode(500));

        assertThrows(RespuestaInvalidaException.class, () -> clienteClient.existeCliente(1));
    }

    @Test
    void servicioClientesNoDisponible() {
        // Se apaga el servidor para simular que servicio-clientes esta caido
        mockServer.stop();

        assertThrows(ServicioNoDisponibleException.class, () -> clienteClient.existeCliente(1));
    }
}
