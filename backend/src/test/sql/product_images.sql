-- Run after Flyway migrations on a disposable PostgreSQL database:
-- psql -v ON_ERROR_STOP=1 -f backend/src/test/sql/product_images.sql
BEGIN;

INSERT INTO products (id, slug, name, brand, type, shape, color, price, currency)
VALUES ('20000000-0000-0000-0000-000000000001', 'image-test', 'Image test', 'Test',
        'eyeglasses', 'Round', 'Black', 1, 'EUR');

DO $$
DECLARE
    test_product UUID := '20000000-0000-0000-0000-000000000001';
BEGIN
    -- A nonempty gallery cannot omit the main selection.
    BEGIN
        INSERT INTO product_images VALUES (test_product, 0, '/test.jpg', 'Test', FALSE, FALSE, 500, 332);
        SET CONSTRAINTS ALL IMMEDIATE;
        RAISE EXCEPTION 'Accepted a gallery without a main thumbnail';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
    SET CONSTRAINTS ALL DEFERRED;

    -- Ordering and rotation selection are independent of the main selection.
    INSERT INTO product_images VALUES
        (test_product, 10, '/side.jpg', 'Side', FALSE, TRUE, 500, 332),
        (test_product, 20, '/front.jpg', 'Front', TRUE, FALSE, 1200, 800),
        (test_product, 30, '/detail.jpg', 'Detail', FALSE, FALSE, 800, 600);
    SET CONSTRAINTS ALL IMMEDIATE;
    SET CONSTRAINTS ALL DEFERRED;

    -- Source dimensions must be positive and present.
    BEGIN
        UPDATE product_images SET width = 0 WHERE product_id = test_product;
        RAISE EXCEPTION 'Accepted zero width';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
    BEGIN
        UPDATE product_images SET height = -1 WHERE product_id = test_product;
        RAISE EXCEPTION 'Accepted negative height';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
    BEGIN
        UPDATE product_images SET width = NULL WHERE product_id = test_product;
        RAISE EXCEPTION 'Accepted missing width';
    EXCEPTION WHEN not_null_violation THEN
        NULL;
    END;
    BEGIN
        UPDATE product_images SET height = NULL WHERE product_id = test_product;
        RAISE EXCEPTION 'Accepted missing height';
    EXCEPTION WHEN not_null_violation THEN
        NULL;
    END;

    BEGIN
        UPDATE product_images SET main_thumbnail = TRUE
        WHERE product_id = test_product AND position = 10;
        SET CONSTRAINTS ALL IMMEDIATE;
        RAISE EXCEPTION 'Accepted two main thumbnails';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
    SET CONSTRAINTS ALL DEFERRED;

    BEGIN
        DELETE FROM product_images WHERE product_id = test_product AND main_thumbnail;
        SET CONSTRAINTS ALL IMMEDIATE;
        RAISE EXCEPTION 'Allowed deletion of the only main thumbnail from a nonempty gallery';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
    SET CONSTRAINTS ALL DEFERRED;

    -- Clearing and replacing the main selection succeeds within one transaction.
    UPDATE product_images SET main_thumbnail = FALSE WHERE product_id = test_product;
    UPDATE product_images SET main_thumbnail = TRUE
    WHERE product_id = test_product AND position = 10;
    SET CONSTRAINTS ALL IMMEDIATE;
    SET CONSTRAINTS ALL DEFERRED;

    -- An empty rotation subset is valid.
    UPDATE product_images SET rotating_thumbnail = FALSE WHERE product_id = test_product;
    SET CONSTRAINTS ALL IMMEDIATE;
    SET CONSTRAINTS ALL DEFERRED;

    -- Removing all images is valid, as is deleting the product itself.
    DELETE FROM product_images WHERE product_id = test_product;
    SET CONSTRAINTS ALL IMMEDIATE;
    SET CONSTRAINTS ALL DEFERRED;
    INSERT INTO product_images VALUES (test_product, 0, '/test.jpg', 'Test', TRUE, TRUE, 500, 332);
    DELETE FROM products WHERE id = test_product;
    SET CONSTRAINTS ALL IMMEDIATE;
END;
$$;

ROLLBACK;
