#!/bin/sh
set -e

# =============================================================================
# PostgreSQL 16 Multi-Database & Least-Privilege User Provisioning Script
# Enforces the Microservices "Database-per-Service" Isolation Pattern Locally:
# Each microservice gets its own database AND its own restricted DB user that
# cannot read or write any other microservice's database.
# =============================================================================

create_isolated_db_and_user() {
    db_name="$1"
    db_user="$2"
    db_password="$3"

    echo "Provisioning isolated database '${db_name}' and restricted role '${db_user}'..."

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        DO \$\$
        BEGIN
            IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${db_user}') THEN
                CREATE ROLE ${db_user} WITH LOGIN PASSWORD '${db_password}';
            END IF;
        END
        \$\$;

        SELECT 'CREATE DATABASE ${db_name} OWNER ${db_user}'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${db_name}')\gexec

        REVOKE ALL ON DATABASE ${db_name} FROM PUBLIC;
        GRANT CONNECT, TEMPORARY ON DATABASE ${db_name} TO ${db_user};
EOSQL

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db_name" <<-EOSQL
        GRANT ALL ON SCHEMA public TO ${db_user};
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${db_user};
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${db_user};
EOSQL
}

create_isolated_db_and_user "$KEYCLOAK_DB_NAME" "$KEYCLOAK_DB_USER" "$KEYCLOAK_DB_PASSWORD"
create_isolated_db_and_user "$USER_DB_NAME" "$USER_DB_USER" "$USER_DB_PASSWORD"
create_isolated_db_and_user "$CATALOG_DB_NAME" "$CATALOG_DB_USER" "$CATALOG_DB_PASSWORD"
create_isolated_db_and_user "$INVENTORY_DB_NAME" "$INVENTORY_DB_USER" "$INVENTORY_DB_PASSWORD"
create_isolated_db_and_user "$COUPON_DB_NAME" "$COUPON_DB_USER" "$COUPON_DB_PASSWORD"
create_isolated_db_and_user "$ORDER_DB_NAME" "$ORDER_DB_USER" "$ORDER_DB_PASSWORD"
create_isolated_db_and_user "$PAYMENT_DB_NAME" "$PAYMENT_DB_USER" "$PAYMENT_DB_PASSWORD"

echo "All 7 isolated microservice databases provisioned successfully."
