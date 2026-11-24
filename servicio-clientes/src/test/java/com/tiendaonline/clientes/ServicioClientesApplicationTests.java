package com.tiendaonline.clientes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tiendaonline.clientes.config.StatsigConfig;

@SpringBootTest
class ServicioClientesApplicationTests {

    // Se reemplaza StatsigConfig por un doble: asi el test no necesita la clave real ni conexion a Statsig
    @MockBean
    StatsigConfig statsigConfig;

    @Test
    void contextLoads() {
    }
}