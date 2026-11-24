package com.tiendaonline.clientes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration 
public class RestClientConfig {
    @Value("${servicios.pedidos.url}")
    private String pedidosUrl;

    // Se construye a partir del RestClient.Builder que Spring ya trae configurado (y no con RestClient.builder()):
    // ese builder esta instrumentado y agrega el traceId a cada llamada saliente, asi la request se puede
    // seguir entre servicios. Con RestClient.builder() el trace se corta.
    @Bean  
    public RestClient pedidosRestClient(RestClient.Builder builder){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        return builder
            .baseUrl(pedidosUrl)
            .requestFactory(factory)
            .build();
    } 
}
