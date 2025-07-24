#!/usr/bin/with-contenv bash
set -e

# Supervisor options → environment with safe defaults
PORT="${PORT:-8883}"
CERT_PATH="${CERT_PATH:-/ssl/certificate.pem}"
KEY_PATH="${KEY_PATH:-/ssl/key.pem}"

echo "Starting MQTT Honeypot on TLS port $PORT"
echo "  Certificate: $CERT_PATH"
echo "  Key:         $KEY_PATH"

exec python3 /usr/src/app/honey.py \
  --port "$PORT" \
  --certfile "$CERT_PATH" \
  --keyfile "$KEY_PATH"
