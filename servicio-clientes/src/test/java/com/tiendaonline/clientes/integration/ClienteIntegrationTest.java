package com.tiendaonline.clientes.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.tiendaonline.clientes.config.StatsigConfig;
import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.repository.ClienteRepository;
import com.tiendaonline.clientes.service.FeatureFlagService;


@SpringBootTest 
@AutoConfigureMockMvc 
@Transactional 
public class ClienteIntegrationTest {
    @Autowired 
    MockMvc mockMvc;
    
    @Autowired 
    ClienteRepository repository;

    @MockBean
    StatsigConfig statsigConfig;

    @MockBean 
    FeatureFlagService featureFlagService;

    static ClientAndServer mockServer = ClientAndServer.startClientAndServer(0);

    @DynamicPropertySource 
    static void configurarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("servicios.pedidos.url", 
        () -> "http://localhost:" + mockServer.getLocalPort());
    }

    @BeforeEach 
    void setUp(){
        mockServer.reset();
    }

    @AfterAll 
    static void tearDown(){
        mockServer.stop();
    }

    @Test 
    void registrarYConsultarCliente() throws Exception{
        when(featureFlagService.isMaskPiiEnabled()).thenReturn(false);

        MvcResult resultado = mockMvc.perform(post("/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
        {
            "nombre": "Cliente Integracion",
            "email": "integracion@test.com"
        }
        """)
        )
        .andExpect(status().isCreated())
        .andReturn();

        assertTrue(repository.existsByEmail("integracion@test.com"));

        Integer id = com.jayway.jsonpath.JsonPath.read(
            resultado.getResponse().getContentAsString(), "$.id");
        
        mockMvc.perform(get("/v1/clientes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.nombre").value("Cliente Integracion"))
        .andExpect(jsonPath("$.email").value("integracion@test.com"))
        .andExpect(jsonPath("$.fechaAlta").exists());
    }

    @Test 
    void consultarPedidosCliente() throws Exception{
        Cliente cliente = new Cliente();
        cliente.setNombre("nombre");
        cliente.setEmail("pedidos-integracion@test.com");
        cliente.setFechaAlta(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        
        Integer id = repository.save(cliente).getId();

        mockServer.when(
            request()
                .withMethod("GET")
                .withPath("/v1/pedidos")
                .withQueryStringParameter("clienteId", id.toString())
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")
        );

        mockMvc.perform(get("/v1/clientes/" + id + "/pedidos"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        mockServer.verify(
            request()
                .withMethod("GET")
                .withPath("/v1/pedidos")
                .withQueryStringParameter("clienteId", id.toString())
        );
    }

}
