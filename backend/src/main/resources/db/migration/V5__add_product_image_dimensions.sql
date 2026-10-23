-- Flush deferred thumbnail checks before altering the table or backfilling it.
SET CONSTRAINTS product_images_main_thumbnail IMMEDIATE;

ALTER TABLE product_images
    ADD COLUMN width INTEGER CHECK (width > 0),
    ADD COLUMN height INTEGER CHECK (height > 0);

-- Measured from the six local source files, including the PNG front view.
UPDATE product_images SET width = 500, height = 332
WHERE url IN (
    '/images/products/misterspex-6536001-a.jpg',
    '/images/products/misterspex-6536001-b.jpg',
    '/images/products/misterspex-6536001-d.png',
    '/images/products/misterspex-6534459-a.jpg',
    '/images/products/misterspex-6534459-b.jpg',
    '/images/products/misterspex-6534459-d.jpg'
);

ALTER TABLE product_images
    ALTER COLUMN width SET NOT NULL,
    ALTER COLUMN height SET NOT NULL;

SET CONSTRAINTS product_images_main_thumbnail DEFERRED;
