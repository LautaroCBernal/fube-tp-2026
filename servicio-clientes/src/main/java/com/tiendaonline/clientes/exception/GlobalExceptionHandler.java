package com.tiendaonline.clientes.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleEmailDuplicado(EmailDuplicadoException exception){
        String mensaje = exception.getMessage();
        ErrorResponse respuesta = new ErrorResponse(409, mensaje);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception){
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();

        StringBuilder mensajes = new StringBuilder("");
        for (FieldError fieldError : fieldErrors) {
            String atributo = fieldError.getField();
            String mensaje = fieldError.getDefaultMessage();
            
            mensajes.append(atributo + ": " + mensaje + ". ");
        }

        ErrorResponse respuesta = new ErrorResponse(400, mensajes.toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @ExceptionHandler(ServicioPedidosNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handleServicioPedidoNoDisponible(ServicioPedidosNoDisponibleException exception){
        String mensaje = exception.getMessage();
        ErrorResponse respuesta = new ErrorResponse(503, mensaje);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(respuesta);
    }

    @ExceptionHandler(RespuestaPedidosInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleRespuestaPedidosInvalida(RespuestaPedidosInvalidaException exception){
        String mensaje = exception.getMessage();
        ErrorResponse respuesta = new ErrorResponse(502, mensaje);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(respuesta);
    }

}
