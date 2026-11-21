package com.tiendaonline.inventario.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.inventario.entity.Producto;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.service.InventarioService;


// Carga solo el contexto de Spring necesario
@WebMvcTest(ProductoController.class)
public class ProductoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventarioService service;

    @Test
    void consultarStockProductoExistente() throws Exception {
        Producto producto = new Producto();
        producto.setId(1);
        producto.setNombre("producto");
        producto.setPrecio(new BigDecimal("1500.00"));
        producto.setCantidadDisponible(7);

        when(service.obtenerProducto(1)).thenReturn(producto);

        mockMvc.perform(get("/v1/productos/1/stock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productoId").value(1))
            .andExpect(jsonPath("$.nombre").value("producto"))
            .andExpect(jsonPath("$.precio").value(1500.00))
            .andExpect(jsonPath("$.cantidadDisponible").value(7));

        verify(service).obtenerProducto(1);
    }

    @Test
    void consultarStockProductoInexistente() throws Exception {
        when(service.obtenerProducto(99)).thenThrow(new ProductoNoEncontradoException(99));

        mockMvc.perform(get("/v1/productos/99/stock"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.mensaje").value("No existe el producto con id 99"));

        verify(service).obtenerProducto(99);
    }
    @Test
    void consultarStockConIdNoNumerico() throws Exception {
        mockMvc.perform(get("/v1/productos/abc/stock"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value("El parametro 'id' tiene un valor invalido"));

        verifyNoInteractions(service);
    }
}
