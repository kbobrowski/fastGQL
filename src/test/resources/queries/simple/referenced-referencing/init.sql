DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;

CREATE TABLE addresses(
  id INT PRIMARY KEY
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  address INT,
  FOREIGN KEY (address) REFERENCES addresses(id)
);

INSERT INTO addresses VALUES (0);
INSERT INTO customers VALUES (0, 0);
