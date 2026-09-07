CREATE KEYSPACE IF NOT EXISTS verifier
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE verifier;

CREATE TABLE IF NOT EXISTS oauth2clientconfig (
  client_id TEXT,
  business_purpose TEXT,
  version TEXT,
  response_mode TEXT,
  request_uri_method TEXT,
  redirect_uri TEXT,
  verifier_info TEXT,
  PRIMARY KEY ((client_id), business_purpose)
);

INSERT INTO oauth2clientconfig (client_id, business_purpose, version, response_mode, request_uri_method, redirect_uri, verifier_info)
VALUES ('client-app-1', 'Payment Processing', '1.0', 'direct_post', 'post', 'https://client1.example.com/redirect', 'verifier-1');

INSERT INTO oauth2clientconfig (client_id, business_purpose, version, response_mode, request_uri_method, redirect_uri, verifier_info)
VALUES ('client-app-2', 'User Authentication', '1.0', 'direct_post', 'get', 'https://client2.example.com/redirect', 'verifier-2');
