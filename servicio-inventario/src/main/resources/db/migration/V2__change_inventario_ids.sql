DROP TABLE reservas;
DROP TABLE productos;

CREATE TABLE productos (
    id                   INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    nombre               VARCHAR(255) NOT NULL,
    precio               NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    cantidad_disponible  INTEGER NOT NULL DEFAULT 0 CHECK (cantidad_disponible >= 0)
);

CREATE TABLE reservas (
    id               INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    producto_id      INT NOT NULL REFERENCES productos(id),
    pedido_id        INT NOT NULL,
    cantidad         INTEGER NOT NULL CHECK (cantidad > 0),
    estado           VARCHAR(20) NOT NULL CHECK (estado IN ('ACTIVA', 'LIBERADA')),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservas_pedido_id ON reservas(pedido_id);
CREATE INDEX idx_reservas_producto_id ON reservas(producto_id);
