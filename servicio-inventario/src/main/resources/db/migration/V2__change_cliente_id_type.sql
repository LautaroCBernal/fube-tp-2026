DROP TABLE Clientes;

CREATE TABLE clientes (
    id          INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    nombre      VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    fecha_alta  TIMESTAMP NOT NULL DEFAULT now()
);