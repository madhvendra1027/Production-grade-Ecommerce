-- Nullable for existing/manual products; populated only for products imported from DummyJSON.
ALTER TABLE products ADD COLUMN dummyjson_id BIGINT;
ALTER TABLE products ADD CONSTRAINT uk_products_dummyjson_id UNIQUE (dummyjson_id);
