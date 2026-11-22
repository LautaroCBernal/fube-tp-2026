package com.tiendaonline.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Un RestClient por servicio remoto, cada uno con su URL base. Los timeouts evitan que un servicio
// colgado deje tambien colgado a pedidos.
@Configuration
public class RestClientConfig {
    @Value("${servicios.clientes.url}")
    private String clientesUrl;

    @Value("${servicios.inventario.url}")
    private String inventarioUrl;

    @Bean
    public RestClient clientesRestClient() {
        return construir(clientesUrl);
    }

    @Bean
    public RestClient inventarioRestClient() {
        return construir(inventarioUrl);
    }

    private RestClient construir(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }
}
