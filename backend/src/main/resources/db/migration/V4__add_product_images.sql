CREATE TABLE product_images (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    url VARCHAR(255) NOT NULL CHECK (btrim(url) <> ''),
    alt_text VARCHAR(255) NOT NULL CHECK (btrim(alt_text) <> ''),
    main_thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
    rotating_thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (product_id, position)
);

-- Deferred so a transaction can replace or reorder a product's entire image list.
CREATE FUNCTION check_product_image_thumbnail() RETURNS TRIGGER AS $$
DECLARE
    affected_product UUID;
BEGIN
    FOR affected_product IN
        SELECT DISTINCT unnest(CASE TG_OP
            WHEN 'DELETE' THEN ARRAY[OLD.product_id]
            WHEN 'INSERT' THEN ARRAY[NEW.product_id]
            ELSE ARRAY[OLD.product_id, NEW.product_id]
        END)
    LOOP
        -- Serialize validation for concurrent changes to the same product.
        PERFORM 1 FROM products WHERE id = affected_product FOR NO KEY UPDATE;
        IF EXISTS (SELECT 1 FROM product_images WHERE product_id = affected_product)
            AND (SELECT count(*) FROM product_images
                 WHERE product_id = affected_product AND main_thumbnail) <> 1 THEN
            RAISE EXCEPTION 'Product % must have exactly one main thumbnail', affected_product
                USING ERRCODE = '23514';
        END IF;
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER product_images_main_thumbnail
    AFTER INSERT OR UPDATE OR DELETE ON product_images
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_product_image_thumbnail();

-- Sample photographs for fictional demo frames; provenance is in docs/product-images.md.
INSERT INTO product_images (product_id, position, url, alt_text, main_thumbnail, rotating_thumbnail) VALUES
('10000000-0000-0000-0000-000000000001', 0, '/images/products/misterspex-6536001-a.jpg', 'Sample round Havana and gold eyeglasses, angled view', TRUE, TRUE),
('10000000-0000-0000-0000-000000000001', 1, '/images/products/misterspex-6536001-b.jpg', 'Sample round Havana and gold eyeglasses, side view', FALSE, TRUE),
('10000000-0000-0000-0000-000000000001', 2, '/images/products/misterspex-6536001-d.png', 'Sample round Havana and gold eyeglasses, front view', FALSE, FALSE),
('10000000-0000-0000-0000-000000000016', 0, '/images/products/misterspex-6534459-a.jpg', 'Sample round Havana sunglasses, angled view', TRUE, TRUE),
('10000000-0000-0000-0000-000000000016', 1, '/images/products/misterspex-6534459-b.jpg', 'Sample round Havana sunglasses, side view', FALSE, TRUE),
('10000000-0000-0000-0000-000000000016', 2, '/images/products/misterspex-6534459-d.jpg', 'Sample round Havana sunglasses, front view', FALSE, FALSE);
