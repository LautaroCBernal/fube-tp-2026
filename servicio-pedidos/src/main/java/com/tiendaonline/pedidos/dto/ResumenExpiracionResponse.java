package com.tiendaonline.pedidos.dto;

// Resumen de una corrida del job de expiracion. Los mismos conteos quedan guardados en BATCH_STEP_EXECUTION
public record ResumenExpiracionResponse(Long jobExecutionId, String estado, long pedidosLeidos,
        long pedidosExpirados, long pedidosConError) {

}
