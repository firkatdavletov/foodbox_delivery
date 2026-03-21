alter table order_items
    add column if not exists sku varchar(255);
