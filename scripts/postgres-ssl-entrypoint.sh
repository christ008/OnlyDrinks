#!/bin/sh
set -eu

TLS_DIR="/var/lib/postgresql/tls"

mkdir -p "$TLS_DIR"
cp /etc/ssl/certs/onlydrinks/db.crt "$TLS_DIR/db.crt"
cp /etc/ssl/certs/onlydrinks/db.key "$TLS_DIR/db.key"
cp /etc/ssl/certs/onlydrinks/ca.crt "$TLS_DIR/ca.crt"

chown postgres:postgres "$TLS_DIR/db.crt" "$TLS_DIR/db.key" "$TLS_DIR/ca.crt"
chmod 644 "$TLS_DIR/db.crt" "$TLS_DIR/ca.crt"
chmod 600 "$TLS_DIR/db.key"

exec docker-entrypoint.sh postgres "$@"
