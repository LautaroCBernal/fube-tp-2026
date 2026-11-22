package com.tiendaonline.pedidos.exception;

public class EjecucionBatchException extends RuntimeException {

    public EjecucionBatchException(Throwable causa) {
        super("No se pudo ejecutar el proceso batch", causa);
    }
}
