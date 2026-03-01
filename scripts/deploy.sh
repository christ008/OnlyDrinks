#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
ENV_EXAMPLE_FILE="$ROOT_DIR/.env.example"

usage() {
  cat <<'EOF'
Usage: scripts/deploy.sh <command>

Commands:
  init-secrets  Create/update .env with strong generated secrets where missing.
  validate      Validate required production configuration and secret strength.
  provision     Init secrets, validate config, generate certs, build image, deploy, and verify health.
  up            Validate config, build image, deploy, and verify health.
  down          Stop and remove compose services.
  logs          Tail compose logs.
  ps            Show compose status.
EOF
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_dependencies() {
  require_cmd docker
  require_cmd openssl
  require_cmd sed
  require_cmd grep
  require_cmd awk
}

ensure_env_file() {
  if [ ! -f "$ENV_FILE" ]; then
    cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
    chmod 600 "$ENV_FILE"
    echo "Created $ENV_FILE from template."
  fi
  chmod 600 "$ENV_FILE"
}

load_env() {
  ensure_env_file
}

upsert_env() {
  local key="$1"
  local value="$2"

  if grep -qE "^${key}=" "$ENV_FILE"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$ENV_FILE"
  else
    printf "%s=%s\n" "$key" "$value" >>"$ENV_FILE"
  fi
}

read_env_file_var() {
  local key="$1"
  if [ ! -f "$ENV_FILE" ]; then
    return 1
  fi
  awk -F= -v key="$key" '$0 ~ "^[[:space:]]*"key"=" {sub(/^[[:space:]]+/, "", $2); print substr($0, index($0, "=")+1)}' "$ENV_FILE" | tail -n1
}

get_var() {
  local key="$1"
  local value="${!key-}"
  if [ -n "$value" ]; then
    printf "%s" "$value"
    return 0
  fi
  read_env_file_var "$key" || true
}

generate_secret() {
  openssl rand -base64 48 | tr -d '\n' | tr '/+' '_-' | cut -c1-48
}

is_weak_secret() {
  local value="${1:-}"
  [ -z "$value" ] || [ "$value" = "changeme" ] || [ "$value" = "password" ] || [ "$value" = "admin" ]
}

init_secrets() {
  load_env

  local db_password
  db_password="$(get_var DB_PASSWORD)"
  if is_weak_secret "$db_password"; then
    upsert_env "DB_PASSWORD" "$(generate_secret)"
    echo "Set strong DB_PASSWORD in .env"
  fi

  local keystore_password
  keystore_password="$(get_var SSL_KEYSTORE_PASSWORD)"
  if is_weak_secret "$keystore_password"; then
    upsert_env "SSL_KEYSTORE_PASSWORD" "$(generate_secret)"
    echo "Set strong SSL_KEYSTORE_PASSWORD in .env"
  fi

  local bootstrap_enabled
  bootstrap_enabled="$(get_var BOOTSTRAP_ADMIN_ENABLED)"
  bootstrap_enabled="${bootstrap_enabled:-true}"
  upsert_env "BOOTSTRAP_ADMIN_ENABLED" "$bootstrap_enabled"

  if [ "$bootstrap_enabled" = "true" ]; then
    [ -n "$(get_var BOOTSTRAP_ADMIN_USERNAME)" ] || upsert_env "BOOTSTRAP_ADMIN_USERNAME" "admin"
    [ -n "$(get_var BOOTSTRAP_ADMIN_NAME)" ] || upsert_env "BOOTSTRAP_ADMIN_NAME" "OnlyDrinks Admin"
    if is_weak_secret "$(get_var BOOTSTRAP_ADMIN_PASSWORD)"; then
      upsert_env "BOOTSTRAP_ADMIN_PASSWORD" "$(generate_secret)"
      echo "Set strong BOOTSTRAP_ADMIN_PASSWORD in .env"
    fi
  fi

  chmod 600 "$ENV_FILE"
}

validate_env() {
  load_env

  local DB_NAME DB_USERNAME DB_PASSWORD SSL_KEYSTORE_PASSWORD BOOTSTRAP_ADMIN_ENABLED BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_NAME BOOTSTRAP_ADMIN_PASSWORD
  DB_NAME="$(get_var DB_NAME)"
  DB_USERNAME="$(get_var DB_USERNAME)"
  DB_PASSWORD="$(get_var DB_PASSWORD)"
  SSL_KEYSTORE_PASSWORD="$(get_var SSL_KEYSTORE_PASSWORD)"
  BOOTSTRAP_ADMIN_ENABLED="$(get_var BOOTSTRAP_ADMIN_ENABLED)"
  BOOTSTRAP_ADMIN_USERNAME="$(get_var BOOTSTRAP_ADMIN_USERNAME)"
  BOOTSTRAP_ADMIN_NAME="$(get_var BOOTSTRAP_ADMIN_NAME)"
  BOOTSTRAP_ADMIN_PASSWORD="$(get_var BOOTSTRAP_ADMIN_PASSWORD)"
  BOOTSTRAP_ADMIN_ENABLED="${BOOTSTRAP_ADMIN_ENABLED:-true}"

  local required_vars=(
    DB_NAME DB_USERNAME DB_PASSWORD
    SSL_KEYSTORE_PASSWORD
  )
  for var in "${required_vars[@]}"; do
    if [ -z "${!var:-}" ]; then
      echo "Missing required variable in .env: $var" >&2
      exit 1
    fi
  done

  if is_weak_secret "${DB_PASSWORD:-}"; then
    echo "DB_PASSWORD is weak/default. Set a strong value in .env." >&2
    exit 1
  fi
  if is_weak_secret "${SSL_KEYSTORE_PASSWORD:-}"; then
    echo "SSL_KEYSTORE_PASSWORD is weak/default. Set a strong value in .env." >&2
    exit 1
  fi

  if [ "$BOOTSTRAP_ADMIN_ENABLED" = "true" ]; then
    local admin_required=(BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_NAME BOOTSTRAP_ADMIN_PASSWORD)
    for var in "${admin_required[@]}"; do
      if [ -z "${!var:-}" ]; then
        echo "Missing required admin bootstrap variable in .env: $var" >&2
        exit 1
      fi
    done
    if is_weak_secret "${BOOTSTRAP_ADMIN_PASSWORD:-}"; then
      echo "BOOTSTRAP_ADMIN_PASSWORD is weak/default. Set a strong value in .env." >&2
      exit 1
    fi
  fi
}

build_image() {
  (cd "$ROOT_DIR" && ./mvnw -DskipTests spring-boot:build-image)
}

compose_up() {
  (cd "$ROOT_DIR" && docker compose up -d --remove-orphans)
}

wait_for_health() {
  local service="$1"
  local max_seconds="${2:-120}"
  local elapsed=0

  local container_id
  container_id="$(cd "$ROOT_DIR" && docker compose ps -q "$service")"
  if [ -z "$container_id" ]; then
    echo "Service '$service' container not found." >&2
    exit 1
  fi

  while [ "$elapsed" -lt "$max_seconds" ]; do
    local status
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id")"
    if [ "$status" = "healthy" ] || [ "$status" = "none" ]; then
      echo "Service '$service' is $status."
      return 0
    fi
    if [ "$status" = "unhealthy" ]; then
      echo "Service '$service' is unhealthy." >&2
      (cd "$ROOT_DIR" && docker compose logs --tail=200 "$service") || true
      exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo "Timed out waiting for '$service' health." >&2
  (cd "$ROOT_DIR" && docker compose logs --tail=200 "$service") || true
  exit 1
}

provision() {
  init_secrets
  validate_env
  (cd "$ROOT_DIR" && ./scripts/generate-certs.sh)
  build_image
  compose_up
  wait_for_health postgres 120
  wait_for_health app 120
  wait_for_health nginx 120
  (cd "$ROOT_DIR" && docker compose ps)
}

cmd="${1:-}"
require_dependencies

case "$cmd" in
  init-secrets)
    init_secrets
    ;;
  validate)
    validate_env
    ;;
  provision)
    provision
    ;;
  up)
    validate_env
    build_image
    compose_up
    wait_for_health postgres 120
    wait_for_health app 120
    wait_for_health nginx 120
    ;;
  down)
    (cd "$ROOT_DIR" && docker compose down)
    ;;
  logs)
    (cd "$ROOT_DIR" && docker compose logs -f)
    ;;
  ps)
    (cd "$ROOT_DIR" && docker compose ps)
    ;;
  *)
    usage
    exit 1
    ;;
esac
