package com.tiendaonline.pedidos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tiendaonline.pedidos.batch.ExpiracionBatchService;
import com.tiendaonline.pedidos.dto.ResumenExpiracionResponse;
import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.ItemPedido;
import com.tiendaonline.pedidos.entity.Pedido;
import com.tiendaonline.pedidos.repository.PedidoRepository;

// Corre el job de expiracion de verdad (reader, processor y writer) contra la base real.
// Lo unico falso es servicio-inventario (MockServer). Requiere PostgreSQL con pedidos_db.
@SpringBootTest
class ExpiracionBatchIntegrationTest {

    private static final ClientAndServer inventarioServer = ClientAndServer.startClientAndServer(0);

    @DynamicPropertySource
    static void urlDeInventario(DynamicPropertyRegistry registry) {
        registry.add("servicios.inventario.url", () -> "http://localhost:" + inventarioServer.getLocalPort());
    }

    @Autowired
    private ExpiracionBatchService service;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    // Guarda un pedido con un item y le pone la antiguedad pedida
    private Integer guardarPedido(EstadoPedido estado, int horasDeAntiguedad) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(5);
        pedido.setEstado(estado);
        pedido.setMontoTotal(new BigDecimal("1500.00"));

        ItemPedido item = new ItemPedido();
        item.setProductoId(2);
        item.setCantidad(3);
        item.setPrecioUnitario(new BigDecimal("500.00"));
        pedido.agregarItem(item);

        Integer id = pedidoRepository.save(pedido).getId();

        // El @PrePersist de Pedido pisa fechaCreacion con la fecha actual, asi que para simular un pedido
        // viejo se la retrocede con un UPDATE directo
        jdbcTemplate.update("UPDATE pedidos SET fecha_creacion = ? WHERE id = ?",
            Timestamp.valueOf(LocalDateTime.now().minusHours(horasDeAntiguedad)), id);

        return id;
    }

    private EstadoPedido estadoEnLaBase(Integer id) {
        return pedidoRepository.findById(id).orElseThrow().getEstado();
    }

    private void inventarioLiberaOk() {
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas/liberar"))
            .respond(response().withStatusCode(200).withHeader("Content-Type", "application/json").withBody("[]"));
    }

    @Test
    void expiraSoloLosPedidosCreadosConMasDe24Horas() {
        Integer vencido = guardarPedido(EstadoPedido.CREADO, 25);
        Integer reciente = guardarPedido(EstadoPedido.CREADO, 1);
        Integer confirmadoViejo = guardarPedido(EstadoPedido.CONFIRMADO, 25);
        Integer canceladoViejo = guardarPedido(EstadoPedido.CANCELADO, 25);
        inventarioLiberaOk();

        ResumenExpiracionResponse resumen = service.ejecutar();

        assertEquals("COMPLETED", resumen.estado());
        assertEquals(1, resumen.pedidosLeidos());
        assertEquals(1, resumen.pedidosExpirados());
        assertEquals(0, resumen.pedidosConError());

        assertEquals(EstadoPedido.EXPIRADO, estadoEnLaBase(vencido));
        assertEquals(EstadoPedido.CREADO, estadoEnLaBase(reciente));
        assertEquals(EstadoPedido.CONFIRMADO, estadoEnLaBase(confirmadoViejo));
        assertEquals(EstadoPedido.CANCELADO, estadoEnLaBase(canceladoViejo));

        // Se libero el stock de ese pedido, y solo de ese
        inventarioServer.verify(request().withMethod("POST").withPath("/v1/reservas/liberar")
            .withQueryStringParameter("pedidoId", String.valueOf(vencido)), VerificationTimes.once());
        inventarioServer.verify(request().withPath("/v1/reservas/liberar"), VerificationTimes.once());
    }

    @Test
    void expiraTodosLosPedidosAunqueHayaMasDeUnChunk() {
        // El chunk es de 10: con 12 pedidos hay dos bloques. Un reader paginado se saltearia pedidos.
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            ids.add(guardarPedido(EstadoPedido.CREADO, 30));
        }
        inventarioLiberaOk();

        ResumenExpiracionResponse resumen = service.ejecutar();

        assertEquals(12, resumen.pedidosLeidos());
        assertEquals(12, resumen.pedidosExpirados());
        for (Integer id : ids) {
            assertEquals(EstadoPedido.EXPIRADO, estadoEnLaBase(id));
        }
    }

    @Test
    void siInventarioEstaCaidoElPedidoNoSeExpiraYSeInformaComoError() {
        Integer vencido = guardarPedido(EstadoPedido.CREADO, 25);
        inventarioServer.when(request().withMethod("POST").withPath("/v1/reservas/liberar"))
            .error(error().withDropConnection(true));

        ResumenExpiracionResponse resumen = service.ejecutar();

        // El job termina bien, pero ese pedido queda sin tocar para la proxima corrida
        assertEquals("COMPLETED", resumen.estado());
        assertEquals(1, resumen.pedidosLeidos());
        assertEquals(0, resumen.pedidosExpirados());
        assertEquals(1, resumen.pedidosConError());
        assertEquals(EstadoPedido.CREADO, estadoEnLaBase(vencido));
    }

    @Test
    void sinPedidosVencidosNoHaceNadaYNoLlamaAInventario() {
        guardarPedido(EstadoPedido.CREADO, 1);

        ResumenExpiracionResponse resumen = service.ejecutar();

        assertEquals("COMPLETED", resumen.estado());
        assertEquals(0, resumen.pedidosLeidos());
        assertEquals(0, resumen.pedidosExpirados());
        assertEquals(0, resumen.pedidosConError());
        inventarioServer.verifyZeroInteractions();
    }
}
