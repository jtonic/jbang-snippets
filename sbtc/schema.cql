CREATE KEYSPACE IF NOT EXISTS sbtc
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE sbtc;

CREATE TABLE IF NOT EXISTS users (
  ssn TEXT,
  first_name TEXT,
  last_name TEXT,
  email TEXT,
  PRIMARY KEY (ssn)
);

INSERT INTO users (ssn, first_name, last_name, email)
VALUES ('111-11-1111', 'Ada', 'Lovelace', 'ada.lovelace@example.com');

INSERT INTO users (ssn, first_name, last_name, email)
VALUES ('222-22-2222', 'Alan', 'Turing', 'alan.turing@example.com');
