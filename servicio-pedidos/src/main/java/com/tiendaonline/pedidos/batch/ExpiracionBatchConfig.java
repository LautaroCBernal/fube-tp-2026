package com.tiendaonline.pedidos.batch;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.Pedido;

import jakarta.persistence.EntityManagerFactory;

// Con Spring Boot 3 / Spring Batch 5 NO se usa @EnableBatchProcessing ni JobBuilderFactory:
// Boot ya configura el JobRepository y el JobLauncher, y los builders reciben el JobRepository directamente.
@Configuration
@EnableScheduling
public class ExpiracionBatchConfig {
    // Cuantos pedidos se procesan por transaccion. Si un bloque falla, solo se deshace ese bloque.
    private static final int TAMANIO_CHUNK = 10;

    // @StepScope hace que este bean se cree cada vez que corre el step y no al arrancar la aplicacion.
    // Sin eso, el limite de "hace mas de 24 hs" se calcularia una sola vez, al iniciar el servicio.
    @Bean
    @StepScope
    public JpaCursorItemReader<Pedido> lectorPedidosVencidos(EntityManagerFactory entityManagerFactory,
            @Value("${batch.expiracion.horas:24}") long horas) {
        LocalDateTime limite = LocalDateTime.now().minusHours(horas);

        // Reader de tipo cursor (y no paginado): como el job cambia los pedidos de CREADO a EXPIRADO mientras
        // lee, un reader paginado correria las paginas y se saltearia pedidos sin avisar.
        return new JpaCursorItemReaderBuilder<Pedido>()
            .name("lectorPedidosVencidos")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT p FROM Pedido p WHERE p.estado = :estado AND p.fechaCreacion < :limite ORDER BY p.id")
            .parameterValues(Map.of("estado", EstadoPedido.CREADO, "limite", limite))
            .build();
    }

    // Guarda los pedidos que devolvio el processor (el @PreUpdate de Pedido actualiza fechaActualizacion)
    @Bean
    public JpaItemWriter<Pedido> escritorPedidos(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Pedido>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    @Bean
    public Step pasoExpirarPedidos(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JpaCursorItemReader<Pedido> lector, ExpirarPedidoProcessor procesador, JpaItemWriter<Pedido> escritor) {
        return new StepBuilder("pasoExpirarPedidos", jobRepository)
            .<Pedido, Pedido>chunk(TAMANIO_CHUNK, transactionManager)
            .reader(lector)
            .processor(procesador)
            .writer(escritor)
            .build();
    }

    @Bean
    public Job jobExpirarPedidos(JobRepository jobRepository, Step pasoExpirarPedidos) {
        return new JobBuilder("jobExpirarPedidos", jobRepository)
            .start(pasoExpirarPedidos)
            .build();
    }
}
