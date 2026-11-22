package com.tiendaonline.pedidos.exception;

import com.tiendaonline.pedidos.entity.EstadoPedido;

public class EstadoPedidoInvalidoException extends RuntimeException {

    public EstadoPedidoInvalidoException(Integer id, EstadoPedido estado, String accion) {
        super("No se puede " + accion + " el pedido " + id + " porque esta en estado " + estado);
    }
}
