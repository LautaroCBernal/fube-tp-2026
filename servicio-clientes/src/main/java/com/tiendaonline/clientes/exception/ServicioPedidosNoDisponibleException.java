package com.tiendaonline.clientes.exception;

public class ServicioPedidosNoDisponibleException extends RuntimeException{
    public ServicioPedidosNoDisponibleException(){
        super("El servicio de pedidos no esta disponible en este momento");
    }
}
