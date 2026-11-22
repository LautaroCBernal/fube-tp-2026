package com.tiendaonline.pedidos.exception;

// El servicio remoto respondio, pero con algo que no esperabamos (error 5xx, cuerpo vacio o mal formado)
public class RespuestaInvalidaException extends RuntimeException {

    public RespuestaInvalidaException(String servicio) {
        super("No se pudo obtener una respuesta valida del servicio de " + servicio);
    }
}
