package com.tiendaonline.inventario.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException() {
        super("No hay stock suficiente para realizar la reserva");
    }
}
