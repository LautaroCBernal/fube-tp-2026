package com.tiendaonline.clientes.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class EmailDuplicadoException extends RuntimeException{
    
    public EmailDuplicadoException(){
        super("El Email ingresado ya esta registrado");
    }
}
