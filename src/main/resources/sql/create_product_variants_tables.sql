-- ========================================
-- TABLA: Líneas de Atributos de Plantillas (Product Template Attribute Line)
-- ========================================
-- Define qué atributos están disponibles para un producto
-- Ej: "Producto Camiseta" tiene atributos "Talla" y "Color"
CREATE TABLE IF NOT EXISTS public.product_template_attribute_line (
    id SERIAL PRIMARY KEY,
    product_template_id INTEGER NOT NULL,
    attribute_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_template_attr_line_template FOREIGN KEY (product_template_id)
        REFERENCES public.product_templates(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_template_attr_line_attribute FOREIGN KEY (attribute_id)
        REFERENCES public.product_attributes(id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_template_attribute UNIQUE (product_template_id, attribute_id)
);

COMMENT ON TABLE public.product_template_attribute_line IS 'Líneas de atributos asignadas a una plantilla de producto';
COMMENT ON COLUMN public.product_template_attribute_line.product_template_id IS 'FK a product_templates';
COMMENT ON COLUMN public.product_template_attribute_line.attribute_id IS 'FK a product_attributes';

-- ========================================
-- TABLA: Variantes de Productos (Product Variants)
-- ========================================
-- Cada combinación de atributos genera una variante
-- También se crea una variante por defecto para productos sin atributos
CREATE TABLE IF NOT EXISTS public.product_variants (
    id SERIAL PRIMARY KEY,
    product_template_id INTEGER NOT NULL,
    sku VARCHAR(25) UNIQUE,
    barcode VARCHAR(100),
    price_extra DECIMAL(12, 4) DEFAULT 0.0,
    cost_extra DECIMAL(12, 4) DEFAULT 0.0,
    stock DECIMAL(12, 2) DEFAULT 0.0,
    status VARCHAR(20) DEFAULT 'active',
    is_default_variant BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_product_variant_template FOREIGN KEY (product_template_id)
        REFERENCES public.product_templates(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_product_variant_status CHECK (status IN ('active', 'inactive', 'discontinued')),
    CONSTRAINT ck_product_variant_stock CHECK (stock >= 0)
);

COMMENT ON TABLE public.product_variants IS 'Variantes específicas de productos (combinaciones de atributos)';
COMMENT ON COLUMN public.product_variants.id IS 'Identificador único de la variante';
COMMENT ON COLUMN public.product_variants.product_template_id IS 'FK a product_templates';
COMMENT ON COLUMN public.product_variants.sku IS 'SKU único de la variante (Stock Keeping Unit)';
COMMENT ON COLUMN public.product_variants.barcode IS 'Código de barras de la variante';
COMMENT ON COLUMN public.product_variants.price_extra IS 'Precio adicional sobre el precio base del template';
COMMENT ON COLUMN public.product_variants.cost_extra IS 'Costo adicional sobre el costo base del template';
COMMENT ON COLUMN public.product_variants.stock IS 'Stock disponible de esta variante';
COMMENT ON COLUMN public.product_variants.status IS 'Estado: active, inactive, discontinued';
COMMENT ON COLUMN public.product_variants.is_default_variant IS 'Indica si es la variante por defecto (para productos sin atributos)';
COMMENT ON COLUMN public.product_variants.deleted_at IS 'Fecha de eliminación lógica (soft delete)';

-- ========================================
-- TABLA: Relación Variante-Valores de Atributos (Product Variant Value Rel)
-- ========================================
-- Define la combinación de valores de atributos de cada variante
-- Ej: Variante "Camiseta-Roja-M" = {color: Rojo, talla: M}
CREATE TABLE IF NOT EXISTS public.product_variant_value_rel (
    variant_id INTEGER NOT NULL,
    value_id INTEGER NOT NULL,

    CONSTRAINT pk_variant_value_rel PRIMARY KEY (variant_id, value_id),
    CONSTRAINT fk_variant_value_rel_variant FOREIGN KEY (variant_id)
        REFERENCES public.product_variants(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_variant_value_rel_value FOREIGN KEY (value_id)
        REFERENCES public.product_attribute_values(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE public.product_variant_value_rel IS 'Relación entre variantes y valores de atributos';
COMMENT ON COLUMN public.product_variant_value_rel.variant_id IS 'FK a product_variants';
COMMENT ON COLUMN public.product_variant_value_rel.value_id IS 'FK a product_attribute_values';

-- ========================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ========================================
CREATE INDEX IF NOT EXISTS idx_template_attr_line_template ON public.product_template_attribute_line(product_template_id);
CREATE INDEX IF NOT EXISTS idx_template_attr_line_attribute ON public.product_template_attribute_line(attribute_id);

CREATE INDEX IF NOT EXISTS idx_product_variants_template ON public.product_variants(product_template_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON public.product_variants(sku) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_variants_barcode ON public.product_variants(barcode) WHERE deleted_at IS NULL AND barcode IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_product_variants_status ON public.product_variants(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_variants_default ON public.product_variants(product_template_id, is_default_variant) WHERE deleted_at IS NULL AND is_default_variant = TRUE;

CREATE INDEX IF NOT EXISTS idx_variant_value_rel_variant ON public.product_variant_value_rel(variant_id);
CREATE INDEX IF NOT EXISTS idx_variant_value_rel_value ON public.product_variant_value_rel(value_id);
