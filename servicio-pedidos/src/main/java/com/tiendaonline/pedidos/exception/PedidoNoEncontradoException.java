package com.tiendaonline.pedidos.exception;

public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Integer id) {
        super("No existe el pedido con id " + id);
    }
}
