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

    @Bean  
    public RestClient pedidosRestClient(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        return RestClient.builder()
            .baseUrl(pedidosUrl)
            .requestFactory(factory)
            .build();
    } 
}
