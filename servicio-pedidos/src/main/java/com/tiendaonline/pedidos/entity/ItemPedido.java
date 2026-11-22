package com.tiendaonline.pedidos.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    // Id del producto en servicio-inventario (no hay FK: es otra base de datos)
    @Column(name = "producto_id")
    private Integer productoId;
    private Integer cantidad;
    // Se guarda el precio al momento de la compra: si el producto cambia de precio, el pedido no se altera
    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;
}
