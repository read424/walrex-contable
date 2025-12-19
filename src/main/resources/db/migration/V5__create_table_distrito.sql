CREATE TABLE district (
    id_district SERIAL PRIMARY KEY,
    cod_district CHAR(4) NOT NULL,
    name_district VARCHAR(100) NOT NULL,

    id_province INTEGER NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_provincia_province
      FOREIGN KEY (id_province)
          REFERENCES province (id_province)
          ON UPDATE CASCADE
          ON DELETE RESTRICT,

    CONSTRAINT district_code_uk UNIQUE (cod_district),
    CONSTRAINT district_name_uk UNIQUE (name_district)
);