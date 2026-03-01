#!/bin/bash
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="$ROOT_DIR/certs"
ENV_FILE="$ROOT_DIR/.env"
mkdir -p "$CERT_DIR"

if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
fi

KEYSTORE_PASSWORD="${SSL_KEYSTORE_PASSWORD:-}"
if [ -z "$KEYSTORE_PASSWORD" ] || [ "$KEYSTORE_PASSWORD" = "changeme" ]; then
    echo "SSL_KEYSTORE_PASSWORD must be set to a non-default value (in .env or environment)." >&2
    exit 1
fi

set_cert_permissions() {
    # Readable cert material for containers, strict perms for private keys.
    chmod 644 "$CERT_DIR"/*.crt "$CERT_DIR"/keystore.p12 "$CERT_DIR"/truststore.p12
    chmod 600 "$CERT_DIR"/db.key "$CERT_DIR"/app.key "$CERT_DIR"/ca.key
}

if [ -f "$CERT_DIR/ca.crt" ] && [ -f "$CERT_DIR/app.crt" ] && [ -f "$CERT_DIR/db.crt" ]; then
    echo "Certificates already exist in $CERT_DIR. Skipping generation."
    set_cert_permissions
    echo "Certificate permissions normalized in $CERT_DIR"
    exit 0
fi

echo "Generating Root CA..."
openssl genrsa -out "$CERT_DIR/ca.key" 4096
openssl req -x509 -new -nodes -key "$CERT_DIR/ca.key" -sha256 -days 3650 \
    -out "$CERT_DIR/ca.crt" \
    -subj "/C=UY/ST=Montevideo/L=Montevideo/O=Christian Tejeda/OU=OnlyDrinks/CN=OnlyDrinks Root CA"

echo "Generating Nginx/App Certificate..."
openssl genrsa -out "$CERT_DIR/app.key" 2048
cat > "$CERT_DIR/app.conf" <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no
[req_distinguished_name]
C = UY
ST = Montevideo
L = Montevideo
O = Christian Tejeda
OU = OnlyDrinks
CN = localhost
[v3_req]
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
DNS.2 = app
DNS.3 = vaadin-app
EOF

openssl req -new -key "$CERT_DIR/app.key" -out "$CERT_DIR/app.csr" -config "$CERT_DIR/app.conf"
openssl x509 -req -in "$CERT_DIR/app.csr" -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" \
    -CAcreateserial -out "$CERT_DIR/app.crt" -days 365 -sha256 -extensions v3_req -extfile "$CERT_DIR/app.conf"

echo "Generating Database Certificate..."
openssl genrsa -out "$CERT_DIR/db.key" 2048
cat > "$CERT_DIR/db.conf" <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no
[req_distinguished_name]
C = UY
ST = Montevideo
L = Montevideo
O = Christian Tejeda
OU = OnlyDrinks
CN = postgres
[v3_req]
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
[alt_names]
DNS.1 = postgres
DNS.2 = localhost
EOF

openssl req -new -key "$CERT_DIR/db.key" -out "$CERT_DIR/db.csr" -config "$CERT_DIR/db.conf"
openssl x509 -req -in "$CERT_DIR/db.csr" -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" \
    -CAcreateserial -out "$CERT_DIR/db.crt" -days 365 -sha256 -extensions v3_req -extfile "$CERT_DIR/db.conf"

echo "Generating PKCS12 keystore for App..."
openssl pkcs12 -export -in "$CERT_DIR/app.crt" -inkey "$CERT_DIR/app.key" \
    -out "$CERT_DIR/keystore.p12" -name onlydrinks -password pass:"$KEYSTORE_PASSWORD"

echo "Generating Truststore for App..."
openssl pkcs12 -export -nokeys -in "$CERT_DIR/ca.crt" \
    -out "$CERT_DIR/truststore.p12" -name caroot -password pass:"$KEYSTORE_PASSWORD"

# Cleanup CSRs and configs
rm "$CERT_DIR"/*.csr "$CERT_DIR"/*.conf "$CERT_DIR"/*.srl

# Important for Postgres: the key must not be world-readable
set_cert_permissions

echo "All certificates generated in $CERT_DIR"
