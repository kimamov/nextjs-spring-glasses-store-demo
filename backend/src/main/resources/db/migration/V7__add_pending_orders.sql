-- REVIEW: Orders are separate from products: the catalog can change after a
-- checkout starts, while the amount and product name agreed here must not change.
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    checkout_token_hash VARCHAR(64) NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    total NUMERIC(38, 2) NOT NULL CHECK (total >= 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (status = 'PENDING_PAYMENT'),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    street VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    postal_code VARCHAR(5) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(2) NOT NULL CHECK (country = 'DE'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 day'),
    -- REVIEW: An application-level "find, then insert" check has a race. Two
    -- requests can both find nothing. This constraint is the final duplicate guard.
    UNIQUE (checkout_token_hash, product_id)
);

-- REVIEW: Only a SHA-256 hash is stored. A database dump must not contain the
-- bearer token that would let someone read the customer's address through the API.
