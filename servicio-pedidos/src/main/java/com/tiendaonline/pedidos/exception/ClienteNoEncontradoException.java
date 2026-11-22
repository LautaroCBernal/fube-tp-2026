package com.tiendaonline.pedidos.exception;

public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(Integer id) {
        super("No existe el cliente con id " + id);
    }
}
