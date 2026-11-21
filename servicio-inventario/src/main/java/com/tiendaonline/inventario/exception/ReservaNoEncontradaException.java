package com.tiendaonline.inventario.exception;

public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException(Integer id) {
        super("No existe la reserva con id " + id);
    }
}
