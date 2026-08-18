-- Pin money columns to a fixed scale and add indexes for soft-delete filtering,
-- sales-history ordering, and order_item foreign-key joins.

ALTER TABLE product ALTER COLUMN price SET DATA TYPE NUMERIC(12, 2);
ALTER TABLE order_item ALTER COLUMN unit_price SET DATA TYPE NUMERIC(12, 2);
ALTER TABLE order_item ALTER COLUMN subtotal SET DATA TYPE NUMERIC(12, 2);
ALTER TABLE sales_order ALTER COLUMN total_amount SET DATA TYPE NUMERIC(12, 2);

CREATE INDEX idx_product_deleted_at ON product (deleted_at);
CREATE INDEX idx_sales_order_order_date ON sales_order (order_date);
CREATE INDEX idx_order_item_order_id ON order_item (order_id);
CREATE INDEX idx_order_item_product_id ON order_item (product_id);
