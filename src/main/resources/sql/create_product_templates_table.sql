-- ========================================
-- ENUM: Tipo de Producto
-- ========================================
DO $$ BEGIN
    CREATE TYPE product_type AS ENUM ('storable', 'consumable', 'service');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

COMMENT ON TYPE product_type IS 'Tipos de producto: storable (almacenable con stock), consumable (consumible sin stock), service (servicio), combo (kit)';

-- ========================================
-- TABLA: Plantillas de Productos (Product Templates)
-- ========================================
CREATE TABLE IF NOT EXISTS public.product_templates (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    internal_reference VARCHAR(100),
    type_product product_type NOT NULL DEFAULT 'storable',

    -- Referencias a otras tablas
    id_category INTEGER,
    id_brand INTEGER,
    id_uom INTEGER NOT NULL,
    id_currency INTEGER NOT NULL,

    -- Precios e impuestos
    sale_price DECIMAL(12, 4) DEFAULT 0.0,
    cost DECIMAL(12, 4) DEFAULT 0.0,
    is_igv_exempt BOOLEAN DEFAULT FALSE,
    tax_rate DECIMAL(5, 4) DEFAULT 0.18,

    -- Propiedades físicas (solo para productos físicos, null para servicios)
    weight DECIMAL(10, 3),
    volume DECIMAL(10, 3),

    -- Control de inventario (false para servicios)
    track_inventory BOOLEAN DEFAULT TRUE,
    use_serial_numbers BOOLEAN DEFAULT FALSE,
    minimum_stock DECIMAL(12, 2),
    maximum_stock DECIMAL(12, 2),
    reorder_point DECIMAL(12, 2),
    lead_time INTEGER,

    -- Información adicional
    image TEXT,
    description TEXT,
    description_sale TEXT,
    barcode VARCHAR(100),
    notes TEXT,

    -- Configuración de uso
    can_be_sold BOOLEAN DEFAULT TRUE,
    can_be_purchased BOOLEAN DEFAULT TRUE,
    allows_price_edit BOOLEAN DEFAULT FALSE,
    has_variants BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'active',

    -- Auditoría y soft delete
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT uk_product_template_internal_ref UNIQUE (internal_reference),
    CONSTRAINT fk_product_template_category FOREIGN KEY (id_category)
        REFERENCES public.product_category(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_product_template_brand FOREIGN KEY (id_brand)
        REFERENCES public.product_brand(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_product_template_uom FOREIGN KEY (id_uom)
        REFERENCES public.product_uom(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_product_template_currency FOREIGN KEY (id_currency)
        REFERENCES public.currencies(id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_product_template_status CHECK (status IN ('active', 'inactive', 'discontinued')),
    CONSTRAINT ck_product_template_prices CHECK (sale_price >= 0 AND cost >= 0),
    CONSTRAINT ck_product_template_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 1),
    CONSTRAINT ck_product_template_stock CHECK (
        minimum_stock IS NULL OR maximum_stock IS NULL OR minimum_stock <= maximum_stock
    )
);

-- ========================================
-- COMENTARIOS DE DOCUMENTACIÓN
-- ========================================
COMMENT ON TABLE public.product_templates IS 'Plantillas maestras de productos (servicios, almacenables y consumibles)';
COMMENT ON COLUMN public.product_templates.id IS 'Identificador único del producto';
COMMENT ON COLUMN public.product_templates.name IS 'Nombre del producto';
COMMENT ON COLUMN public.product_templates.internal_reference IS 'Referencia interna única (SKU, código)';
COMMENT ON COLUMN public.product_templates.type_product IS 'Tipo de producto: storable, consumable, service';
COMMENT ON COLUMN public.product_templates.id_category IS 'FK a tabla product_category (opcional)';
COMMENT ON COLUMN public.product_templates.id_brand IS 'FK a tabla product_brand (opcional)';
COMMENT ON COLUMN public.product_templates.id_uom IS 'FK a tabla product_uom (unidad de medida) - requerido';
COMMENT ON COLUMN public.product_templates.id_currency IS 'FK a tabla currencies (moneda) - requerido';
COMMENT ON COLUMN public.product_templates.sale_price IS 'Precio de venta';
COMMENT ON COLUMN public.product_templates.cost IS 'Costo del producto';
COMMENT ON COLUMN public.product_templates.is_igv_exempt IS 'Exento de IGV (impuesto)';
COMMENT ON COLUMN public.product_templates.tax_rate IS 'Tasa de impuesto (0.18 = 18%)';
COMMENT ON COLUMN public.product_templates.weight IS 'Peso en kg (null para servicios)';
COMMENT ON COLUMN public.product_templates.volume IS 'Volumen en m³ (null para servicios)';
COMMENT ON COLUMN public.product_templates.track_inventory IS 'Controlar inventario (false para servicios)';
COMMENT ON COLUMN public.product_templates.use_serial_numbers IS 'Usar números de serie';
COMMENT ON COLUMN public.product_templates.minimum_stock IS 'Stock mínimo';
COMMENT ON COLUMN public.product_templates.maximum_stock IS 'Stock máximo';
COMMENT ON COLUMN public.product_templates.reorder_point IS 'Punto de reorden';
COMMENT ON COLUMN public.product_templates.lead_time IS 'Tiempo de entrega en días';
COMMENT ON COLUMN public.product_templates.image IS 'URL o base64 de la imagen del producto';
COMMENT ON COLUMN public.product_templates.description IS 'Descripción general del producto';
COMMENT ON COLUMN public.product_templates.description_sale IS 'Descripción para ventas';
COMMENT ON COLUMN public.product_templates.barcode IS 'Código de barras';
COMMENT ON COLUMN public.product_templates.notes IS 'Notas adicionales';
COMMENT ON COLUMN public.product_templates.can_be_sold IS 'Puede ser vendido';
COMMENT ON COLUMN public.product_templates.can_be_purchased IS 'Puede ser comprado';
COMMENT ON COLUMN public.product_templates.allows_price_edit IS 'Permite editar precio durante la venta (solo para servicios)';
COMMENT ON COLUMN public.product_templates.has_variants IS 'Tiene variantes de producto';
COMMENT ON COLUMN public.product_templates.status IS 'Estado: active, inactive, discontinued';
COMMENT ON COLUMN public.product_templates.deleted_at IS 'Fecha de eliminación lógica (soft delete)';

-- ========================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ========================================
CREATE INDEX IF NOT EXISTS idx_product_templates_name ON public.product_templates(name) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_internal_ref ON public.product_templates(internal_reference) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_type ON public.product_templates(type_product) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_category ON public.product_templates(id_category) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_brand ON public.product_templates(id_brand) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_status ON public.product_templates(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_can_be_sold ON public.product_templates(can_be_sold) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_product_templates_barcode ON public.product_templates(barcode) WHERE deleted_at IS NULL AND barcode IS NOT NULL;

-- ========================================
-- DATOS INICIALES: Producto de Servicio de Ejemplo
-- ========================================
-- Nota: Asegúrate de tener las FKs correctas (id_uom, id_currency) antes de ejecutar esto
INSERT INTO public.product_templates (
    name,
    internal_reference,
    type_product,
    id_uom,
    id_currency,
    sale_price,
    cost,
    tax_rate,
    is_igv_exempt,
    description,
    description_sale,
    can_be_sold,
    can_be_purchased,
    allows_price_edit,
    track_inventory,
    has_variants,
    status
)
SELECT
    'Consultoría Técnica Especializada',
    'SERV-001',
    'service',
    (SELECT id FROM public.product_uom WHERE code_uom = 'UNI' LIMIT 1),  -- Asumiendo UNI existe
    (SELECT id FROM public.currencies WHERE code = 'PEN' LIMIT 1),        -- Asumiendo PEN existe
    150.00,
    50.00,
    0.18,
    FALSE,
    'Servicio de análisis y diseño de software a medida',
    'Asesoría técnica especializada en arquitectura de sistemas',
    TRUE,
    FALSE,
    FALSE,  -- Por defecto no permite edición de precio
    FALSE,  -- Los servicios NO tienen inventario
    FALSE,
    'active'
WHERE NOT EXISTS (
    SELECT 1 FROM public.product_templates WHERE internal_reference = 'SERV-001'
);
