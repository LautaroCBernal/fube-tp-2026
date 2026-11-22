package com.tiendaonline.pedidos.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiracionScheduler {
    private final ExpiracionBatchService service;

    public ExpiracionScheduler(ExpiracionBatchService service) {
        this.service = service;
    }

    // Todos los dias a las 2:00 AM (segundo minuto hora dia mes dia-de-semana). Se puede cambiar en application.yml
    @Scheduled(cron = "${batch.expiracion.cron:0 0 2 * * *}")
    public void ejecutarNocturno() {
        service.ejecutar();
    }
}
