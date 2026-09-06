CREATE KEYSPACE IF NOT EXISTS verifier
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE verifier;

CREATE TABLE IF NOT EXISTS oauth2clientconfig (
  client_id TEXT PRIMARY KEY,
  businessPurpose TEXT
);

INSERT INTO oauth2clientconfig (client_id, businessPurpose) VALUES ('client-app-1', 'Payment Processing');
INSERT INTO oauth2clientconfig (client_id, businessPurpose) VALUES ('client-app-2', 'User Authentication');