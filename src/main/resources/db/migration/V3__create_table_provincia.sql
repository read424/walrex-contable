CREATE TABLE province (
    id_province SERIAL PRIMARY KEY,
    cod_province CHAR(4) NOT NULL,
    name_province VARCHAR(100) NOT NULL,

    id_departament INTEGER NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_provincia_departamento
       FOREIGN KEY (id_departament)
           REFERENCES departament (id_departament)
           ON UPDATE CASCADE
           ON DELETE RESTRICT,

    CONSTRAINT province_code_uk UNIQUE (cod_province),
    CONSTRAINT province_name_uk UNIQUE (name_province)
);