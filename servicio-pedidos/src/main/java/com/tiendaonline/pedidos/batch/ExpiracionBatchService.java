package com.tiendaonline.pedidos.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import com.tiendaonline.pedidos.dto.ResumenExpiracionResponse;
import com.tiendaonline.pedidos.exception.EjecucionBatchException;

// Unico punto por el que se lanza el job: lo usan el cron nocturno y el endpoint manual
@Service
public class ExpiracionBatchService {
    private static final Logger log = LoggerFactory.getLogger(ExpiracionBatchService.class);

    private final JobLauncher jobLauncher;
    private final Job jobExpirarPedidos;

    public ExpiracionBatchService(JobLauncher jobLauncher, Job jobExpirarPedidos) {
        this.jobLauncher = jobLauncher;
        this.jobExpirarPedidos = jobExpirarPedidos;
    }

    public ResumenExpiracionResponse ejecutar() {
        // Cada corrida necesita parametros distintos: si no, Spring Batch considera que esa instancia
        // del job ya se completo y se niega a ejecutarla de nuevo
        JobParameters parametros = new JobParametersBuilder()
            .addLong("inicio", System.currentTimeMillis())
            .toJobParameters();

        JobExecution ejecucion;
        try {
            // Se ejecuta de forma sincronica: devuelve cuando el job termino
            ejecucion = jobLauncher.run(jobExpirarPedidos, parametros);
        } catch (JobExecutionException e) {
            log.error("No se pudo ejecutar el job de expiracion de pedidos", e);
            throw new EjecucionBatchException(e);
        }

        // leidos = pedidos vencidos que encontro, expirados = los que se guardaron,
        // con error = los que se descartaron porque inventario no pudo liberar su stock
        long leidos = ejecucion.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long expirados = ejecucion.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        long conError = ejecucion.getStepExecutions().stream().mapToLong(StepExecution::getFilterCount).sum();

        ResumenExpiracionResponse resumen = new ResumenExpiracionResponse(
            ejecucion.getId(), ejecucion.getStatus().name(), leidos, expirados, conError);

        log.info("Resumen de la expiracion de pedidos: job={}, estado={}, leidos={}, expirados={}, con error={}",
            resumen.jobExecutionId(), resumen.estado(), leidos, expirados, conError);

        return resumen;
    }
}
