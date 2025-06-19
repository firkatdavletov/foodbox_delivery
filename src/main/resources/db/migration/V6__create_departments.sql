DROP TABLE IF EXISTS departments CASCADE;

CREATE TABLE departments (
    id SERIAL PRIMARY KEY,
    address INTEGER NOT NULL REFERENCES addresses(id)
);