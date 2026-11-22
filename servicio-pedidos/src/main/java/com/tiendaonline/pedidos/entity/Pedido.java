package com.tiendaonline.pedidos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    // Id del cliente en servicio-clientes (no hay FK: es otra base de datos)
    @Column(name = "cliente_id")
    private Integer clienteId;
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;
    @Column(name = "monto_total")
    private BigDecimal montoTotal;
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Los items viven y mueren con el pedido: se guardan en cascada junto con el
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    public void agregarItem(ItemPedido item) {
        item.setPedido(this);
        items.add(item);
    }

    // Las fechas las maneja la entidad para no repetir la logica en cada servicio
    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}
