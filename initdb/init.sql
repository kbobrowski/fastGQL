DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255),
  house_number INT
);

CREATE TABLE customers(
                          id INT PRIMARY KEY,
                          first_name VARCHAR(255),
                          last_name VARCHAR(255),
                          email VARCHAR(255),
                          address_id INTEGER,
                          FOREIGN KEY (address_id) REFERENCES addresses (id)
);
INSERT INTO addresses VALUES (101, 'Astreet', 5);
INSERT INTO addresses VALUES (102, 'Bstreet', 6);
INSERT INTO customers VALUES (101, 'John', 'Adam', 'john@adam.com', 101);
INSERT INTO customers VALUES (102, 'Uli', 'Werk', 'uli@werk.com', 102);

