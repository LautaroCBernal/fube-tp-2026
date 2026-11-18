package com.tiendaonline.clientes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import com.statsig.sdk.Statsig;
import com.statsig.sdk.StatsigOptions;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;


@Configuration 
public class StatsigConfig {
    @Value("${statsig.server-key}")
    private String serverKey;

    @PostConstruct 
    public void inicializarStatsig() throws Exception{
        // Configuracion
        StatsigOptions options = new StatsigOptions();
        // Inicializa Statsig con el servidor
        Statsig.initializeAsync(serverKey, options).get();
    }

    @PreDestroy 
    public void cerrarStatsig() throws Exception{
        Statsig.shutdown();
    }

}
