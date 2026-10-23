-- Replace the old samples for the 60 seeded products, preserving other products.
DELETE FROM product_images
WHERE product_id BETWEEN '10000000-0000-0000-0000-000000000001'::uuid
                     AND '10000000-0000-0000-0000-000000000060'::uuid;

-- Reuse the five supplied sets by catalog color; navy uses the black sample.
WITH demo_products AS (
    SELECT id, name,
        CASE color
            WHEN 'Tortoiseshell' THEN '02-havanna'
            WHEN 'Champagne' THEN '03-gold'
            WHEN 'Clear' THEN '04-transparent-grau'
            WHEN 'Olive' THEN '05-olivgruen'
            ELSE '01-schwarz'
        END AS image_group
    FROM products
    WHERE id BETWEEN '10000000-0000-0000-0000-000000000001'::uuid
                 AND '10000000-0000-0000-0000-000000000060'::uuid
)
INSERT INTO product_images
    (product_id, position, url, alt_text, width, height, main_thumbnail, rotating_thumbnail)
SELECT id, image_number - 1,
    '/images/products/brille-' || image_group || '-bild-0' || image_number || '.png',
    name || ' - sample frame image ' || image_number,
    384, CASE WHEN image_group = '01-schwarz' THEN 204 ELSE 205 END,
    image_number = 1, TRUE
FROM demo_products CROSS JOIN generate_series(1, 4) AS images(image_number);
