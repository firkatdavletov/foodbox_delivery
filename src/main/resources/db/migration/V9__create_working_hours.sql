DROP TABLE IF EXISTS working_hours CASCADE;

CREATE TABLE working_hours (
    id SERIAL PRIMARY KEY,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    department_id INTEGER NOT NULL REFERENCES departments(id)
);