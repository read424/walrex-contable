-- ========================================
-- TABLA 1: Categorías de Unidades de Medida
-- ========================================
CREATE TABLE IF NOT EXISTS public.product_category_uom (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT uk_category_uom_code UNIQUE (code)
);

COMMENT ON TABLE public.product_category_uom IS 'Categorías de unidades de medida (longitud, peso, volumen, etc.)';
COMMENT ON COLUMN public.product_category_uom.code IS 'Código único de la categoría (ej: LENGTH, WEIGHT, VOLUME)';
COMMENT ON COLUMN public.product_category_uom.name IS 'Nombre de la categoría';
COMMENT ON COLUMN public.product_category_uom.description IS 'Descripción de la categoría';
COMMENT ON COLUMN public.product_category_uom.is_active IS 'Estado activo/inactivo de la categoría';
COMMENT ON COLUMN public.product_category_uom.deleted_at IS 'Fecha de eliminación lógica (soft delete)';

-- ========================================
-- TABLA 2: Unidades de Medida de Productos
-- ========================================
CREATE TABLE IF NOT EXISTS public.product_uom (
    id SERIAL PRIMARY KEY,
    code_uom VARCHAR(10) NOT NULL,
    name_uom VARCHAR(100) NOT NULL,
    id_category_uom INTEGER NOT NULL,
    factor DECIMAL(12, 6) DEFAULT 1.0,
    rounding_precision DECIMAL(12, 6) DEFAULT 0.01,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT uk_product_uom_code UNIQUE (code_uom),
    CONSTRAINT fk_product_uom_category FOREIGN KEY (id_category_uom)
        REFERENCES public.product_category_uom(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE public.product_uom IS 'Unidades de medida para productos (kg, m, L, etc.)';
COMMENT ON COLUMN public.product_uom.code_uom IS 'Código único de la unidad de medida (ej: KG, M, L, UNI)';
COMMENT ON COLUMN public.product_uom.name_uom IS 'Nombre de la unidad de medida (ej: Kilogramo, Metro, Litro)';
COMMENT ON COLUMN public.product_uom.id_category_uom IS 'FK a tabla product_category_uom';
COMMENT ON COLUMN public.product_uom.factor IS 'Factor de conversión a la unidad base de la categoría';
COMMENT ON COLUMN public.product_uom.rounding_precision IS 'Precisión de redondeo para cálculos';
COMMENT ON COLUMN public.product_uom.is_active IS 'Estado activo/inactivo de la unidad';
COMMENT ON COLUMN public.product_uom.deleted_at IS 'Fecha de eliminación lógica (soft delete)';

-- ========================================
-- TABLA 3: Atributos de Productos
-- ========================================
CREATE TABLE IF NOT EXISTS public.product_attributes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    display_type VARCHAR(20) NOT NULL DEFAULT 'select',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT uk_product_attribute_name UNIQUE (name),
    CONSTRAINT ck_display_type CHECK (display_type IN ('select', 'radio', 'color', 'text'))
);

COMMENT ON TABLE public.product_attributes IS 'Atributos configurables de productos (talla, color, material, etc.)';
COMMENT ON COLUMN public.product_attributes.id IS 'Identificador único del atributo';
COMMENT ON COLUMN public.product_attributes.name IS 'Nombre del atributo';
COMMENT ON COLUMN public.product_attributes.display_type IS 'Tipo de visualización: select, radio, color, text';
COMMENT ON COLUMN public.product_attributes.is_active IS 'Estado activo/inactivo del atributo';
COMMENT ON COLUMN public.product_attributes.deleted_at IS 'Fecha de eliminación lógica (soft delete)';

-- ========================================
-- TABLA 4: Valores de Atributos de Productos
-- ========================================
CREATE TABLE IF NOT EXISTS public.product_attribute_values (
    id SERIAL PRIMARY KEY,
    attribute_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    html_color VARCHAR(7),
    sequence INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_attribute_value_attribute FOREIGN KEY (attribute_id)
        REFERENCES public.product_attributes(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_attribute_value UNIQUE (attribute_id, name)
);

COMMENT ON TABLE public.product_attribute_values IS 'Valores específicos de atributos (S, M, L para talla / Rojo, Azul para color)';
COMMENT ON COLUMN public.product_attribute_values.id IS 'Identificador único del valor';
COMMENT ON COLUMN public.product_attribute_values.attribute_id IS 'FK a tabla product_attributes';
COMMENT ON COLUMN public.product_attribute_values.name IS 'Nombre del valor del atributo';
COMMENT ON COLUMN public.product_attribute_values.html_color IS 'Código hexadecimal de color (#RRGGBB) cuando display_type es color';
COMMENT ON COLUMN public.product_attribute_values.sequence IS 'Orden de visualización de los valores';
COMMENT ON COLUMN public.product_attribute_values.is_active IS 'Estado activo/inactivo del valor';
COMMENT ON COLUMN public.product_attribute_values.deleted_at IS 'Fecha de eliminación lógica (soft delete)';

-- ========================================
-- DATOS INICIALES: Categorías UOM
-- ========================================
INSERT INTO public.product_category_uom (code, name, description, is_active)
VALUES
    ('UNIT', 'Unidad', 'Unidades discretas (piezas, items)', TRUE),
    ('WEIGHT', 'Peso', 'Medidas de peso y masa', TRUE),
    ('LENGTH', 'Longitud', 'Medidas de longitud y distancia', TRUE),
    ('VOLUME', 'Volumen', 'Medidas de volumen y capacidad', TRUE),
    ('AREA', 'Área', 'Medidas de superficie', TRUE),
    ('TIME', 'Tiempo', 'Medidas de duración temporal', TRUE)
ON CONFLICT (code) DO NOTHING;

-- ========================================
-- DATOS INICIALES: Unidades de Medida
-- ========================================
INSERT INTO public.product_uom (code_uom, name_uom, id_category_uom, factor, is_active)
SELECT 'UNI', 'Unidad', id, 1.0, TRUE FROM public.product_category_uom WHERE code = 'UNIT'
UNION ALL
SELECT 'KG', 'Kilogramo', id, 1.0, TRUE FROM public.product_category_uom WHERE code = 'WEIGHT'
UNION ALL
SELECT 'G', 'Gramo', id, 0.001, TRUE FROM public.product_category_uom WHERE code = 'WEIGHT'
UNION ALL
SELECT 'LB', 'Libra', id, 0.453592, TRUE FROM public.product_category_uom WHERE code = 'WEIGHT'
UNION ALL
SELECT 'M', 'Metro', id, 1.0, TRUE FROM public.product_category_uom WHERE code = 'LENGTH'
UNION ALL
SELECT 'CM', 'Centímetro', id, 0.01, TRUE FROM public.product_category_uom WHERE code = 'LENGTH'
UNION ALL
SELECT 'L', 'Litro', id, 1.0, TRUE FROM public.product_category_uom WHERE code = 'VOLUME'
UNION ALL
SELECT 'ML', 'Mililitro', id, 0.001, TRUE FROM public.product_category_uom WHERE code = 'VOLUME'
ON CONFLICT (code_uom) DO NOTHING;

-- ========================================
-- DATOS INICIALES: Atributos de Productos
-- ========================================
INSERT INTO public.product_attributes (name, display_type, is_active)
VALUES
    ('Talla', 'select', TRUE),
    ('Color', 'color', TRUE),
    ('Material', 'select', TRUE)
ON CONFLICT (name) DO NOTHING;

-- ========================================
-- DATOS INICIALES: Valores de Atributos
-- ========================================
-- Valores para Talla
INSERT INTO public.product_attribute_values (attribute_id, name, sequence, is_active)
SELECT id, 'XS', 1, TRUE FROM public.product_attributes WHERE name = 'Talla'
UNION ALL
SELECT id, 'S', 2, TRUE FROM public.product_attributes WHERE name = 'Talla'
UNION ALL
SELECT id, 'M', 3, TRUE FROM public.product_attributes WHERE name = 'Talla'
UNION ALL
SELECT id, 'L', 4, TRUE FROM public.product_attributes WHERE name = 'Talla'
UNION ALL
SELECT id, 'XL', 5, TRUE FROM public.product_attributes WHERE name = 'Talla'
UNION ALL
SELECT id, 'XXL', 6, TRUE FROM public.product_attributes WHERE name = 'Talla'
ON CONFLICT (attribute_id, name) DO NOTHING;

-- Valores para Color
INSERT INTO public.product_attribute_values (attribute_id, name, html_color, sequence, is_active)
SELECT id, 'Rojo', '#FF0000', 1, TRUE FROM public.product_attributes WHERE name = 'Color'
UNION ALL
SELECT id, 'Azul', '#0000FF', 2, TRUE FROM public.product_attributes WHERE name = 'Color'
UNION ALL
SELECT id, 'Verde', '#00FF00', 3, TRUE FROM public.product_attributes WHERE name = 'Color'
UNION ALL
SELECT id, 'Negro', '#000000', 4, TRUE FROM public.product_attributes WHERE name = 'Color'
UNION ALL
SELECT id, 'Blanco', '#FFFFFF', 5, TRUE FROM public.product_attributes WHERE name = 'Color'
ON CONFLICT (attribute_id, name) DO NOTHING;

-- Valores para Material
INSERT INTO public.product_attribute_values (attribute_id, name, sequence, is_active)
SELECT id, 'Algodón', 1, TRUE FROM public.product_attributes WHERE name = 'Material'
UNION ALL
SELECT id, 'Poliéster', 2, TRUE FROM public.product_attributes WHERE name = 'Material'
UNION ALL
SELECT id, 'Cuero', 3, TRUE FROM public.product_attributes WHERE name = 'Material'
UNION ALL
SELECT id, 'Lana', 4, TRUE FROM public.product_attributes WHERE name = 'Material'
ON CONFLICT (attribute_id, name) DO NOTHING;

-- ========================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ========================================
CREATE INDEX IF NOT EXISTS idx_product_category_uom_active ON public.product_category_uom(is_active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_category_uom_code ON public.product_category_uom(code) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_product_uom_active ON public.product_uom(is_active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_uom_code ON public.product_uom(code_uom) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_uom_category ON public.product_uom(id_category_uom) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_product_attributes_active ON public.product_attributes(is_active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_attributes_display_type ON public.product_attributes(display_type) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_product_attribute_values_active ON public.product_attribute_values(is_active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_attribute_values_attribute ON public.product_attribute_values(attribute_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_attribute_values_sequence ON public.product_attribute_values(attribute_id, sequence) WHERE deleted_at IS NULL;
