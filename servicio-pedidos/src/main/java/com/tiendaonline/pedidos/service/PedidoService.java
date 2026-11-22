package com.tiendaonline.pedidos.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendaonline.pedidos.client.ClienteClient;
import com.tiendaonline.pedidos.client.InventarioClient;
import com.tiendaonline.pedidos.dto.ItemPedidoRequest;
import com.tiendaonline.pedidos.dto.ItemPedidoResponse;
import com.tiendaonline.pedidos.dto.PedidoRequest;
import com.tiendaonline.pedidos.dto.PedidoResponse;
import com.tiendaonline.pedidos.dto.StockResponse;
import com.tiendaonline.pedidos.entity.EstadoPedido;
import com.tiendaonline.pedidos.entity.ItemPedido;
import com.tiendaonline.pedidos.entity.Pedido;
import com.tiendaonline.pedidos.exception.ClienteNoEncontradoException;
import com.tiendaonline.pedidos.exception.EstadoPedidoInvalidoException;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.repository.PedidoRepository;

@Service
public class PedidoService {
    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ClienteClient clienteClient;
    private final InventarioClient inventarioClient;

    public PedidoService(PedidoRepository pedidoRepository, ClienteClient clienteClient,
            InventarioClient inventarioClient) {
        this.pedidoRepository = pedidoRepository;
        this.clienteClient = clienteClient;
        this.inventarioClient = inventarioClient;
    }

    // Este metodo NO es @Transactional a proposito: hace llamadas HTTP a otros servicios y no queremos
    // tener una conexion de base de datos tomada mientras esperamos. Ademas, un rollback de la base
    // no podria deshacer una reserva ya hecha en inventario: por eso la compensacion es manual.
    public PedidoResponse crearPedido(PedidoRequest request) {
        // 1. Validar que el cliente exista (si no, no se toca nada)
        if (!clienteClient.existeCliente(request.clienteId())) {
            throw new ClienteNoEncontradoException(request.clienteId());
        }

        // 2. Armar el pedido consultando el precio de cada producto a inventario.
        //    Todavia no se reservo ni guardo nada: si un producto no existe, se corta aca sin efectos.
        Pedido pedido = new Pedido();
        pedido.setClienteId(request.clienteId());
        pedido.setEstado(EstadoPedido.CREADO);

        BigDecimal montoTotal = BigDecimal.ZERO;
        for (ItemPedidoRequest itemRequest : request.items()) {
            StockResponse stock = inventarioClient.obtenerStock(itemRequest.productoId());

            ItemPedido item = new ItemPedido();
            item.setProductoId(itemRequest.productoId());
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(stock.precio());
            pedido.agregarItem(item);

            montoTotal = montoTotal.add(stock.precio().multiply(BigDecimal.valueOf(itemRequest.cantidad())));
        }
        pedido.setMontoTotal(montoTotal);

        // 3. Guardar el pedido para obtener su id (inventario necesita el pedidoId para reservar)
        Pedido guardado = pedidoRepository.save(pedido);

        // 4. Reservar el stock de cada item. Si alguna reserva falla, se deshacen las anteriores
        try {
            for (ItemPedido item : guardado.getItems()) {
                inventarioClient.reservar(item.getProductoId(), guardado.getId(), item.getCantidad());
            }
        } catch (RuntimeException e) {
            compensar(guardado);
            throw e;
        }

        return aResponse(guardado);
    }

    // Deshace un pedido que no se pudo completar: libera lo que se haya reservado y borra el pedido
    // (la consigna dice que si falla, "el pedido no se crea").
    private void compensar(Pedido pedido) {
        try {
            inventarioClient.liberarReservasDePedido(pedido.getId());
        } catch (RuntimeException e) {
            // Limitacion conocida: si inventario tampoco responde, quedan reservas activas sin liberar.
            // El pedido se conserva como CANCELADO para que quede registro y se pueda liberar despues.
            log.error("No se pudieron liberar las reservas del pedido {}; queda CANCELADO", pedido.getId(), e);
            pedido.setEstado(EstadoPedido.CANCELADO);
            pedidoRepository.save(pedido);
            return;
        }

        pedidoRepository.deleteById(pedido.getId());
    }

    // @Transactional porque los items son LAZY: hay que leerlos con la sesion de Hibernate abierta,
    // y como open-in-view esta desactivado, la transaccion es lo que la mantiene abierta
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPedido(Integer id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new PedidoNoEncontradoException(id));

        return aResponse(pedido);
    }

    // Si el cliente no tiene pedidos devuelve una lista vacia (no es un error)
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidosPorCliente(Integer clienteId) {
        return pedidoRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId).stream()
            .map(this::aResponse)
            .toList();
    }

    // Igual que crearPedido, NO es @Transactional: llama por HTTP a inventario. Los items se traen junto con
    // el pedido (findConItemsById) porque son LAZY y mas adelante no hay una sesion de Hibernate abierta.
    public PedidoResponse cancelarPedido(Integer id) {
        Pedido pedido = pedidoRepository.findConItemsById(id)
            .orElseThrow(() -> new PedidoNoEncontradoException(id));

        if (pedido.getEstado() != EstadoPedido.CREADO && pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new EstadoPedidoInvalidoException(id, pedido.getEstado(), "cancelar");
        }

        // Primero se libera el stock y recien despues se cambia el estado. Si inventario falla, el pedido
        // queda como estaba y se puede reintentar (liberar es idempotente). Al reves, podria quedar un
        // pedido CANCELADO con el stock reservado para siempre.
        inventarioClient.liberarReservasDePedido(pedido.getId());

        pedido.setEstado(EstadoPedido.CANCELADO);
        return aResponse(pedidoRepository.save(pedido));
    }

    // La consigna no define como se confirma un pedido, pero el batch habla de pedidos "sin confirmar",
    // asi que hace falta una forma de hacerlo. No llama a otros servicios, por eso si puede ser @Transactional.
    @Transactional
    public PedidoResponse confirmarPedido(Integer id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new PedidoNoEncontradoException(id));

        if (pedido.getEstado() != EstadoPedido.CREADO) {
            throw new EstadoPedidoInvalidoException(id, pedido.getEstado(), "confirmar");
        }

        pedido.setEstado(EstadoPedido.CONFIRMADO);
        // saveAndFlush (y no save) para que se ejecute ya el @PreUpdate y la respuesta traiga la fecha nueva
        return aResponse(pedidoRepository.saveAndFlush(pedido));
    }

    // Se devuelven DTOs y no entidades: asi la API no queda atada al modelo de la base
    private PedidoResponse aResponse(Pedido pedido) {
        List<ItemPedidoResponse> items = pedido.getItems().stream()
            .map(this::aResponse)
            .toList();

        return new PedidoResponse(pedido.getId(), pedido.getClienteId(), pedido.getEstado(),
            pedido.getMontoTotal(), pedido.getFechaCreacion(), pedido.getFechaActualizacion(), items);
    }

    private ItemPedidoResponse aResponse(ItemPedido item) {
        return new ItemPedidoResponse(item.getId(), item.getProductoId(), item.getCantidad(), item.getPrecioUnitario());
    }
}
