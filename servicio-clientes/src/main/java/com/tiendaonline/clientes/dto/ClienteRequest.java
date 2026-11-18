package com.tiendaonline.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(@NotBlank(message = "No puede estar vacío") @Size(max = 255, message = "No puede superar los 100 caracteres") String nombre, @NotBlank(message = "No puede estar vacío") @Size(max = 255, message = "El formato del correo es inválido") String email) {
    
}
