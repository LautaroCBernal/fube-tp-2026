package com.tiendaonline.inventario.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "producto_id")
    private Integer productoId;
    // Id del pedido en servicio-pedidos (no hay FK: es otra base de datos)
    @Column(name = "pedido_id")
    private Integer pedidoId;
    private Integer cantidad;
    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
