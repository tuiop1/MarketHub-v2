-- Run against the maintenance database as the RDS administrator, not an app user.
-- psql reads passwords from the container environment; format(%L) quotes them safely.
\getenv account_password ACCOUNT_DB_PASSWORD
\getenv catalog_password CATALOG_DB_PASSWORD
\getenv cart_password CART_DB_PASSWORD
\getenv order_password ORDER_DB_PASSWORD
\getenv payment_password PAYMENT_DB_PASSWORD
\getenv keycloak_password KEYCLOAK_DB_PASSWORD

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', name, password)
FROM (VALUES
  ('markethub_account', :'account_password'),
  ('markethub_catalog', :'catalog_password'),
  ('markethub_cart', :'cart_password'),
  ('markethub_order', :'order_password'),
  ('markethub_payment', :'payment_password'),
  ('markethub_keycloak', :'keycloak_password')
) AS roles(name, password)
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = name)
\gexec

-- Membership lets the RDS admin create databases owned by each application role.
SELECT format('GRANT %I TO %I', name, current_user)
FROM (VALUES ('markethub_account'), ('markethub_catalog'), ('markethub_cart'),
             ('markethub_order'), ('markethub_payment'), ('markethub_keycloak')) AS roles(name)
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', name, owner)
FROM (VALUES ('account', 'markethub_account'), ('catalog', 'markethub_catalog'),
             ('cart', 'markethub_cart'), ('orders', 'markethub_order'),
             ('payment', 'markethub_payment'), ('keycloak', 'markethub_keycloak')) AS databases(name, owner)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = name)
\gexec

SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', name)
FROM (VALUES ('account'), ('catalog'), ('cart'), ('orders'), ('payment'), ('keycloak')) AS databases(name)
\gexec

-- Reruns leave existing passwords and data intact. Rotate credentials explicitly.
