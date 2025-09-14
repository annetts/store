-- baseline schema (Flyway)

CREATE TABLE IF NOT EXISTS items (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    price NUMERIC(12,2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
    );

CREATE TABLE IF NOT EXISTS sales (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE ON UPDATE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    price_at_sale NUMERIC(12,2) NOT NULL CHECK (price_at_sale >= 0),
    total NUMERIC(12,2) NOT NULL,
    sold_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_sales_item_id ON sales(item_id);
