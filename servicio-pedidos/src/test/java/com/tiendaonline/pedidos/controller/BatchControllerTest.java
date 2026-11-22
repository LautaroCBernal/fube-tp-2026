package com.tiendaonline.pedidos.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.pedidos.batch.ExpiracionBatchService;
import com.tiendaonline.pedidos.dto.ResumenExpiracionResponse;
import com.tiendaonline.pedidos.exception.EjecucionBatchException;

@WebMvcTest(BatchController.class)
public class BatchControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpiracionBatchService service;

    @Test
    void expirarPedidosDevuelveElResumen() throws Exception {
        when(service.ejecutar()).thenReturn(new ResumenExpiracionResponse(3L, "COMPLETED", 4, 3, 1));

        mockMvc.perform(post("/v1/batch/expirar-pedidos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobExecutionId").value(3))
            .andExpect(jsonPath("$.estado").value("COMPLETED"))
            .andExpect(jsonPath("$.pedidosLeidos").value(4))
            .andExpect(jsonPath("$.pedidosExpirados").value(3))
            .andExpect(jsonPath("$.pedidosConError").value(1));

        verify(service).ejecutar();
    }

    @Test
    void expirarPedidosSiNoSePuedeLanzarElJobDevuelve500() throws Exception {
        when(service.ejecutar()).thenThrow(new EjecucionBatchException(new JobExecutionException("fallo")));

        mockMvc.perform(post("/v1/batch/expirar-pedidos"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.mensaje").value("No se pudo ejecutar el proceso batch"));
    }
}
