package com.tiendaonline.pedidos.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.tiendaonline.pedidos.client.InventarioClient;
import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.Pedido;
import com.tiendaonline.pedidos.exception.RespuestaInvalidaException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;

// Recibe un pedido vencido (CREADO hace mas de 24 hs) y lo deja listo para guardarse como EXPIRADO
@Component
public class ExpirarPedidoProcessor implements ItemProcessor<Pedido, Pedido> {
    private static final Logger log = LoggerFactory.getLogger(ExpirarPedidoProcessor.class);

    private final InventarioClient inventarioClient;

    public ExpirarPedidoProcessor(InventarioClient inventarioClient) {
        this.inventarioClient = inventarioClient;
    }

    @Override
    public Pedido process(Pedido pedido) {
        // Igual que al cancelar: primero se libera el stock y recien despues se cambia el estado
        try {
            inventarioClient.liberarReservasDePedido(pedido.getId());
        } catch (ServicioNoDisponibleException | RespuestaInvalidaException e) {
            // Devolver null le dice a Spring Batch que descarte este pedido (no se guarda). Queda CREADO
            // y lo vuelve a tomar la corrida de la noche siguiente. Reintentar es seguro porque liberar
            // es idempotente. Cuenta como "filtrado" y sale como error en el resumen.
            log.warn("No se pudo expirar el pedido {}: {}", pedido.getId(), e.getMessage());
            return null;
        }

        pedido.setEstado(EstadoPedido.EXPIRADO);
        return pedido;
    }
}
