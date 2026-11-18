package com.tiendaonline.clientes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.exception.EmailDuplicadoException;
import com.tiendaonline.clientes.exception.ServicioPedidosNoDisponibleException;
import com.tiendaonline.clientes.service.ClienteService;
import com.tiendaonline.clientes.service.FeatureFlagService;
import com.tiendaonline.clientes.service.PiiMaskingService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Carga solo el contexto de Spring necesario
@WebMvcTest(ClienteController.class)
public class ClienteControllerTest {
    // Instancia MockMvc (Peticiones HTTP)
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteService service;

    @MockBean
    private FeatureFlagService  featureFlagService;

    @MockBean
    private PiiMaskingService piiMaskingService;

    @Test
    void registrarClienteValido() throws Exception{
        // Prepara las respuestas del service simulado
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setNombre("nombre");
        cliente.setEmail("nombre@gmail.com");
        cliente.setFechaAlta(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        
        when(service.registrarCliente("nombre", "nombre@gmail.com")).thenReturn(cliente);

        // Ejecuta la peticion definiendo el json del body
        mockMvc.perform(post("/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "nombre": "nombre",
                    "email": "nombre@gmail.com"
                }    
            """)
        )
        // Verifica los campos del Cliente cargado
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nombre").value("nombre"))
        .andExpect(jsonPath("$.email").value("nombre@gmail.com"))
        .andExpect(jsonPath("$.fechaAlta").value("2026-01-01T00:00:00"));
        verify(service).registrarCliente("nombre", "nombre@gmail.com");
    }


    @Test
    void registrarClienteEmailDuplicado() throws Exception{
        // Prepara las respuestas del service simulado        
        when(service.registrarCliente("nombre", "nombre@gmail.com")).thenThrow(new EmailDuplicadoException());

        // Ejecuta la peticion definiendo el json del body
        mockMvc.perform(post("/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "nombre": "nombre",
                    "email": "nombre@gmail.com"
                }    
            """)
        )
        // Verifica los campos de la excepcion
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.mensaje").value("El Email ingresado ya esta registrado"));
        verify(service).registrarCliente("nombre", "nombre@gmail.com");

    }

    @Test
    void registrarClienteEmailVacio() throws Exception{
        // Ejecuta la peticion definiendo el json del body
        mockMvc.perform(post("/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "nombre": "nombre",
                    "email": ""
                }    
            """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.mensaje").value("email: No puede estar vacío. "));
        // Verifica que no se haya ejecutado el registrarCliente en el service
        verifyNoInteractions(service);
    }

    @Test
    void registrarClienteNombreVacio() throws Exception{
        // Ejecuta la peticion definiendo el json del body
        mockMvc.perform(post("/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "nombre": "",
                    "email": "nombre@gmail.com"
                }    
            """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.mensaje").value("nombre: No puede estar vacío. "));
        // Verifica que no se haya ejecutado el registrarCliente en el service
        verifyNoInteractions(service);
    }

    @Test
    void obtenerClienteExistente() throws Exception{
        // Prepara las respuestas del service simulado
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setNombre("nombre");
        cliente.setEmail("nombre@gmail.com");
        cliente.setFechaAlta(LocalDateTime.of(2026, 1, 1, 0, 0, 0));

        when(service.obtenerCliente(1))
            .thenReturn(Optional.of(cliente));
        
        when(featureFlagService.isMaskPiiEnabled())
            .thenReturn(false);

        // Ejecuta la peticion y verifica el resultado
        mockMvc.perform(get("/v1/clientes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("nombre"))
            .andExpect(jsonPath("$.email").value("nombre@gmail.com"))
            .andExpect(jsonPath("$.fechaAlta").value("2026-01-01T00:00:00"));

        verify(service).obtenerCliente(1);
        verifyNoInteractions(piiMaskingService);
    }

    @Test
    void obtenerClienteInexistente() throws Exception{
        // Prepara las respuestas del service simulado
        when(service.obtenerCliente(0))
            .thenReturn(Optional.empty());

        // Ejecuta la peticion y verifica el resultado
        mockMvc.perform(get("/v1/clientes/0"))
            .andExpect(status().isNotFound());

        verify(service).obtenerCliente(0);
    }

    @Test
    void obtenerPedidosClienteExistente() throws Exception {
        when(service.obtenerPedidosPorCliente(1))
            .thenReturn(Optional.of(List.of()));

        mockMvc.perform(get("/v1/clientes/1/pedidos"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verify(service).obtenerPedidosPorCliente(1);
    }

    @Test
    void obtenerPedidosClienteInexistente() throws Exception {
        when(service.obtenerPedidosPorCliente(1))
            .thenReturn(Optional.empty());
    
        mockMvc.perform(get("/v1/clientes/1/pedidos"))
            .andExpect(status().isNotFound());
    
        verify(service).obtenerPedidosPorCliente(1);
    }

    @Test
    void obtenerPedidosServicioNoDisponible() throws Exception {
        when(service.obtenerPedidosPorCliente(1))
            .thenThrow(new ServicioPedidosNoDisponibleException());
    
        mockMvc.perform(get("/v1/clientes/1/pedidos"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.mensaje").value("El servicio de pedidos no esta disponible en este momento"));
    
        verify(service).obtenerPedidosPorCliente(1);
    }
}
