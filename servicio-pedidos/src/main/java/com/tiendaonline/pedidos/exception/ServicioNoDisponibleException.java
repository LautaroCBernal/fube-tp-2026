package com.tiendaonline.pedidos.exception;

// El servicio remoto no respondio: esta caido, no se pudo conectar o se agoto el timeout
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String servicio) {
        super("El servicio de " + servicio + " no esta disponible en este momento");
    }
}
