CREATE TABLE departament (
    id_departament SERIAL PRIMARY KEY,

    cod_departament CHAR(2) NOT NULL UNIQUE,       -- UBIGEO: 01, 02, ..., 25
    name_departament VARCHAR(100) NOT NULL,

    status BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,

    CONSTRAINT departament_name_uk UNIQUE (name_departament),
    CONSTRAINT departament_code_uk UNIQUE (cod_departament)
);