package com.tiendaonline.inventario.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleProductoNoEncontrado(ProductoNoEncontradoException exception) {
        ErrorResponse respuesta = new ErrorResponse(404, exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleReservaNoEncontrada(ReservaNoEncontradaException exception) {
        ErrorResponse respuesta = new ErrorResponse(404, exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleStockInsuficiente(StockInsuficienteException exception) {
        ErrorResponse respuesta = new ErrorResponse(409, exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
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

    // JSON mal formado o con tipos incorrectos (ej: "cantidad": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBodyInvalido(HttpMessageNotReadableException exception) {
        ErrorResponse respuesta = new ErrorResponse(400, "El cuerpo de la peticion es invalido");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }
    // Falta un @RequestParam obligatorio (ej: POST /v1/reservas/liberar sin pedidoId)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroFaltante(MissingServletRequestParameterException exception) {
        ErrorResponse respuesta = new ErrorResponse(400,
            "Falta el parametro obligatorio: " + exception.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // Un parametro o variable de ruta con tipo incorrecto (ej: /productos/abc/stock)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException exception) {
        ErrorResponse respuesta = new ErrorResponse(400,
            "El parametro '" + exception.getName() + "' tiene un valor invalido");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }
}
