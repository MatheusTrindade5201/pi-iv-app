#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

STORE_FILE="${1:-release.jks}"
ALIAS="expediente"

if [[ -e "$STORE_FILE" || -e keystore.properties ]]; then
  echo "Já existe $STORE_FILE ou keystore.properties; apague-os antes de gerar outra chave." >&2
  exit 1
fi

read -r -s -p "Senha da keystore (mín. 6 caracteres): " PASSWORD
echo
read -r -s -p "Repita a senha: " CONFIRM
echo
if [[ "$PASSWORD" != "$CONFIRM" || ${#PASSWORD} -lt 6 ]]; then
  echo "Senhas diferentes ou curtas demais." >&2
  exit 1
fi

keytool -genkeypair -v \
  -keystore "$STORE_FILE" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASSWORD" -keypass "$PASSWORD" \
  -dname "CN=Expediente, OU=PI IV, O=Univesp, C=BR"

cat > keystore.properties <<PROPS
storeFile=$STORE_FILE
storePassword=$PASSWORD
keyAlias=$ALIAS
keyPassword=$PASSWORD
PROPS
chmod 600 keystore.properties "$STORE_FILE"

echo "Pronto: $STORE_FILE e keystore.properties (ambos fora do git). Guarde a senha e faça backup da keystore."
echo "Para o CI: base64 -w0 $STORE_FILE  -> secret RELEASE_KEYSTORE_BASE64"
