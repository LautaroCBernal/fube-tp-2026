package com.tiendaonline.inventario.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.tiendaonline.inventario.entity.Producto;
import com.tiendaonline.inventario.repository.ProductoRepository;
import com.tiendaonline.inventario.repository.ReservaRepository;

// Prueba el servicio completo contra la base real (inventario no llama a otros servicios,
// por eso no hace falta MockServer). Requiere inventario_db levantada.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class InventarioIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductoRepository productoRepository;

    @Autowired
    ReservaRepository reservaRepository;

    // Crea un producto propio para no depender de los datos de la migracion V3
    private Integer crearProducto(int stock) {
        Producto producto = new Producto();
        producto.setNombre("producto integracion");
        producto.setPrecio(new BigDecimal("100.00"));
        producto.setCantidadDisponible(stock);
        return productoRepository.save(producto).getId();
    }

    private int stockActual(Integer productoId) {
        return productoRepository.findById(productoId).orElseThrow().getCantidadDisponible();
    }

    private String cuerpoReserva(Integer productoId, int pedidoId, int cantidad) {
        return """
            {
                "productoId": %d,
                "pedidoId": %d,
                "cantidad": %d
            }
            """.formatted(productoId, pedidoId, cantidad);
    }

    @Test
    void reservarYLiberarRestauraElStock() throws Exception {
        Integer productoId = crearProducto(10);

        MvcResult resultado = mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoReserva(productoId, 1, 4)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("ACTIVA"))
            .andReturn();

        // La reserva descuenta stock
        assertEquals(6, stockActual(productoId));

        Integer reservaId = com.jayway.jsonpath.JsonPath.read(
            resultado.getResponse().getContentAsString(), "$.id");

        // Liberar devuelve el stock
        mockMvc.perform(post("/v1/reservas/" + reservaId + "/liberar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("LIBERADA"));
        assertEquals(10, stockActual(productoId));

        // Liberar de nuevo es idempotente: no repone stock por segunda vez
        mockMvc.perform(post("/v1/reservas/" + reservaId + "/liberar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("LIBERADA"));
        assertEquals(10, stockActual(productoId));
    }

    @Test
    void reservarSinStockSuficienteNoModificaNada() throws Exception {
        Integer productoId = crearProducto(2);
        long reservasAntes = reservaRepository.count();

        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoReserva(productoId, 1, 5)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        assertEquals(2, stockActual(productoId));
        assertEquals(reservasAntes, reservaRepository.count());
    }

    @Test
    void reservarProductoInexistente() throws Exception {
        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoReserva(999999, 1, 1)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void liberarReservasDePedidoLiberaTodasLasDelPedido() throws Exception {
        Integer producto1 = crearProducto(10);
        Integer producto2 = crearProducto(20);

        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoReserva(producto1, 77, 3)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/reservas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoReserva(producto2, 77, 5)))
            .andExpect(status().isCreated());

        assertEquals(7, stockActual(producto1));
        assertEquals(15, stockActual(producto2));

        mockMvc.perform(post("/v1/reservas/liberar").param("pedidoId", "77"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        assertEquals(10, stockActual(producto1));
        assertEquals(20, stockActual(producto2));
    }

    @Test
    void consultarStock() throws Exception {
        Integer productoId = crearProducto(9);

        mockMvc.perform(get("/v1/productos/" + productoId + "/stock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productoId").value(productoId))
            .andExpect(jsonPath("$.cantidadDisponible").value(9))
            .andExpect(jsonPath("$.precio").value(100.00));
    }

    @Test
    void consultarStockProductoInexistente() throws Exception {
        mockMvc.perform(get("/v1/productos/999999/stock"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
