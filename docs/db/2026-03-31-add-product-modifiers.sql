-- Adds catalog modifiers, cart item modifier snapshots, and order item modifier snapshots.

CREATE TABLE IF NOT EXISTS modifier_groups (
    id uuid PRIMARY KEY,
    code varchar(128) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    min_selected integer NOT NULL,
    max_selected integer NOT NULL,
    is_required boolean NOT NULL,
    is_active boolean NOT NULL,
    sort_order integer NOT NULL
);

CREATE TABLE IF NOT EXISTS modifier_options (
    id uuid PRIMARY KEY,
    group_id uuid NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    code varchar(128) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    price_type varchar(32) NOT NULL,
    price bigint NOT NULL,
    application_scope varchar(32) NOT NULL,
    is_default boolean NOT NULL,
    is_active boolean NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT uk_modifier_options_group_code UNIQUE (group_id, code)
);

CREATE TABLE IF NOT EXISTS product_modifier_groups (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES catalog_products(id) ON DELETE CASCADE,
    modifier_group_id uuid NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    sort_order integer NOT NULL,
    is_active boolean NOT NULL,
    CONSTRAINT uk_product_modifier_groups UNIQUE (product_id, modifier_group_id)
);

CREATE TABLE IF NOT EXISTS cart_item_modifiers (
    id uuid PRIMARY KEY,
    cart_item_id uuid NOT NULL REFERENCES cart_items(id) ON DELETE CASCADE,
    modifier_group_id uuid NOT NULL REFERENCES modifier_groups(id),
    modifier_option_id uuid NOT NULL REFERENCES modifier_options(id),
    group_code_snapshot varchar(128) NOT NULL,
    group_name_snapshot varchar(255) NOT NULL,
    option_code_snapshot varchar(128) NOT NULL,
    option_name_snapshot varchar(255) NOT NULL,
    application_scope_snapshot varchar(32) NOT NULL,
    price_snapshot bigint NOT NULL,
    quantity integer NOT NULL
);

CREATE TABLE IF NOT EXISTS order_item_modifiers (
    id uuid PRIMARY KEY,
    order_item_id uuid NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    modifier_group_id uuid NOT NULL REFERENCES modifier_groups(id),
    modifier_option_id uuid NOT NULL REFERENCES modifier_options(id),
    group_code_snapshot varchar(128) NOT NULL,
    group_name_snapshot varchar(255) NOT NULL,
    option_code_snapshot varchar(128) NOT NULL,
    option_name_snapshot varchar(255) NOT NULL,
    application_scope_snapshot varchar(32) NOT NULL,
    price_snapshot bigint NOT NULL,
    quantity integer NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_modifier_options_group_id ON modifier_options(group_id);
CREATE INDEX IF NOT EXISTS idx_product_modifier_groups_product_id ON product_modifier_groups(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_modifiers_cart_item_id ON cart_item_modifiers(cart_item_id);
CREATE INDEX IF NOT EXISTS idx_order_item_modifiers_order_item_id ON order_item_modifiers(order_item_id);
