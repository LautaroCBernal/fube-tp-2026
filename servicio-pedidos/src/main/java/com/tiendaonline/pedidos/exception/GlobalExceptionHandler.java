package com.tiendaonline.pedidos.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Mismo formato de error {status, mensaje} que clientes e inventario.
// Las excepciones de negocio (cliente inexistente, stock insuficiente, estado invalido...) se agregan en los pasos siguientes.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handlePedidoNoEncontrado(PedidoNoEncontradoException exception) {
        ErrorResponse respuesta = new ErrorResponse(404, exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();

        StringBuilder mensajes = new StringBuilder("");
        for (FieldError fieldError : fieldErrors) {
            mensajes.append(fieldError.getField() + ": " + fieldError.getDefaultMessage() + ". ");
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

    // Falta un @RequestParam obligatorio (ej: GET /v1/pedidos sin clienteId)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroFaltante(MissingServletRequestParameterException exception) {
        ErrorResponse respuesta = new ErrorResponse(400,
            "Falta el parametro obligatorio: " + exception.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // Un parametro o variable de ruta con tipo incorrecto (ej: /pedidos/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException exception) {
        ErrorResponse respuesta = new ErrorResponse(400,
            "El parametro '" + exception.getName() + "' tiene un valor invalido");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // ---------- Errores de negocio ----------

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleClienteNoEncontrado(ClienteNoEncontradoException exception) {
        ErrorResponse respuesta = new ErrorResponse(404, exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleProductoNoEncontrado(ProductoNoEncontradoException exception) {
        ErrorResponse respuesta = new ErrorResponse(404, exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleStockInsuficiente(StockInsuficienteException exception) {
        ErrorResponse respuesta = new ErrorResponse(409, exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    // ---------- Fallas al comunicarse con otros servicios ----------

    // 503: el otro servicio no esta disponible
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handleServicioNoDisponible(ServicioNoDisponibleException exception) {
        ErrorResponse respuesta = new ErrorResponse(503, exception.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(respuesta);
    }

    // 502: el otro servicio respondio algo que no se pudo interpretar
    @ExceptionHandler(RespuestaInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleRespuestaInvalida(RespuestaInvalidaException exception) {
        ErrorResponse respuesta = new ErrorResponse(502, exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(respuesta);
    }
}
