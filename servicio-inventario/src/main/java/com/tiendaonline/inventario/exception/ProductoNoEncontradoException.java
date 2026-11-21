package com.tiendaonline.inventario.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Integer id) {
        super("No existe el producto con id " + id);
    }
}
