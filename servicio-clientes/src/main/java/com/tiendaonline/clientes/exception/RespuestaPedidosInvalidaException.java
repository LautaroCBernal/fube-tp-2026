package com.tiendaonline.clientes.exception;

public class RespuestaPedidosInvalidaException extends RuntimeException{
    public RespuestaPedidosInvalidaException(){
        super("No se pudo obtener una respuesta valida del servicio de pedidos");
    }
}
