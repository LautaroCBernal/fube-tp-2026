package com.tiendaonline.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Un RestClient por servicio remoto, cada uno con su URL base. Los timeouts evitan que un servicio
// colgado deje tambien colgado a pedidos.
// Se construyen a partir del RestClient.Builder que Spring ya trae configurado (y no con RestClient.builder()):
// ese builder esta instrumentado y agrega el traceId a cada llamada saliente, asi la request se puede seguir
// entre servicios. Con RestClient.builder() el trace se corta.
@Configuration
public class RestClientConfig {
    @Value("${servicios.clientes.url}")
    private String clientesUrl;

    @Value("${servicios.inventario.url}")
    private String inventarioUrl;

    @Bean
    public RestClient clientesRestClient(RestClient.Builder builder) {
        return construir(builder, clientesUrl);
    }

    @Bean
    public RestClient inventarioRestClient(RestClient.Builder builder) {
        return construir(builder, inventarioUrl);
    }

    private RestClient construir(RestClient.Builder builder, String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        return builder
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }
}
