package com.tiendaonline.clientes.exception;

public class EmailDuplicadoException extends RuntimeException{
    
    public EmailDuplicadoException(){
        super("El Email ingresado ya esta registrado");
    }
}
