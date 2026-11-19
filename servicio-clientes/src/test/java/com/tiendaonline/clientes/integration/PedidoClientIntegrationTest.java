package com.tiendaonline.clientes.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import org.mockserver.integration.ClientAndServer;
import org.springframework.web.client.RestClient;

import com.tiendaonline.clientes.client.PedidoClient;
import com.tiendaonline.clientes.dto.PedidoResponse;
import com.tiendaonline.clientes.exception.RespuestaPedidosInvalidaException;
import com.tiendaonline.clientes.exception.ServicioPedidosNoDisponibleException;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PedidoClientIntegrationTest {
    private ClientAndServer mockServer;
    private PedidoClient pedidoClient;

    @BeforeEach 
    public void setUp(){
        mockServer = ClientAndServer.startClientAndServer(0);
        String url = "http://localhost:" + mockServer.getLocalPort();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        
        RestClient restClient = RestClient.builder()
                        .baseUrl(url)
                        .requestFactory(factory)
                        .build();

        pedidoClient = new PedidoClient(restClient);
    }

    @AfterEach 
    public void tearDown(){
        if(mockServer != null && mockServer.isRunning())
            mockServer.stop();
    }

    @Test 
    void obtenerPedidosPorClienteExitoso(){
        String bodyRespuesta = """
        [
            {
                "id": 1,
                "clienteId": 1,
                "estado": "CREADO",
                "montoTotal": 1500.00,
                "fechaCreacion": "2026-11-19T10:00:00",
                "fechaActualizacion": "2026-11-19T10:00:00",
                "items": [
                    {
                        "id": 1,
                        "productoId": 2,
                        "cantidad": 3,
                        "precioUnitario": 500.00
                    }
                ]
            }
        ]
        """;
        
        mockServer.when(
            request()
                .withMethod("GET")
                .withPath("/v1/pedidos")
                .withQueryStringParameter("clienteId", "1")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(bodyRespuesta)
        );

        List<PedidoResponse> pedidos = pedidoClient.obtenerPedidosPorCliente(1);

        assertNotNull(pedidos);
        assertEquals(1, pedidos.size());
        assertEquals(1, pedidos.get(0).id());
        assertEquals(1, pedidos.get(0).clienteId());
        assertEquals("CREADO", pedidos.get(0).estado());
        assertEquals(1, pedidos.get(0).items().size());
    }

    @Test
    void obtenerPedidosListaVacia(){
        mockServer.when(
            request()
                .withMethod("GET")
                .withPath("/v1/pedidos")
                .withQueryStringParameter("clienteId", "1")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")
        );

        List<PedidoResponse> pedidos = pedidoClient.obtenerPedidosPorCliente(1);

        assertNotNull(pedidos);
        assertTrue(pedidos.isEmpty());
    }

    @Test
    void obtenerPedidosRespuestaInvalida(){
        mockServer.when(
            request()
                .withMethod("GET")
                .withPath("/v1/pedidos")
                .withQueryStringParameter("clienteId", "1")
        ).respond(
            response()
                .withStatusCode(500)
        );

        assertThrows(
            RespuestaPedidosInvalidaException.class,
            () -> pedidoClient.obtenerPedidosPorCliente(1)
        );
    }

    @Test 
    void obtenerPedidosServicioNoDisponible(){
        mockServer.stop();
        
        assertThrows(
            ServicioPedidosNoDisponibleException.class,
            () -> pedidoClient.obtenerPedidosPorCliente(1)
        );
    }
}
