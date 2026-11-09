DROP TABLE items_pedido;
DROP TABLE pedidos;

CREATE TABLE pedidos (
    id                    INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    cliente_id            INT NOT NULL,
    estado                VARCHAR(20) NOT NULL CHECK (estado IN ('CREADO', 'CONFIRMADO', 'CANCELADO', 'EXPIRADO')),
    monto_total           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    fecha_creacion        TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE items_pedido (
    id                INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    pedido_id         INT NOT NULL REFERENCES pedidos(id),
    producto_id       INT NOT NULL,
    cantidad          INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario   NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_pedidos_cliente_id ON pedidos(cliente_id);
CREATE INDEX idx_items_pedido_pedido_id ON items_pedido(pedido_id);
