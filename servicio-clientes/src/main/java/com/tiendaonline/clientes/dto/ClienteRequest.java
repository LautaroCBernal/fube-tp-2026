package com.tiendaonline.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(@NotBlank @Size(max = 255) String nombre, @NotBlank @Size(max = 255) String email) {
    
}
