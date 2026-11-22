package com.tiendaonline.pedidos.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException() {
        super("No hay stock suficiente para realizar el pedido");
    }
}
