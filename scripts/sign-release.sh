#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 unsigned.apk signed.apk private-signing-directory" >&2
  exit 2
fi

: "${APKSIGNER_JAR:?Set APKSIGNER_JAR to Android SDK build-tools/lib/apksigner.jar}"
unsigned_apk="$1"
signed_apk="$2"
signing_directory="$3"
if [[ -e "$signed_apk" ]]; then
  echo "Output already exists: $signed_apk" >&2
  exit 1
fi

java -jar "$APKSIGNER_JAR" sign \
  --ks "$signing_directory/offline-counter.p12" \
  --ks-type PKCS12 \
  --ks-key-alias offline-counter \
  --ks-pass "file:$signing_directory/keystore.password" \
  --key-pass "file:$signing_directory/keystore.password" \
  --v1-signing-enabled false \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --v4-signing-enabled false \
  --out "$signed_apk" "$unsigned_apk"
java -jar "$APKSIGNER_JAR" verify --verbose --print-certs "$signed_apk"
