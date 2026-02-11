#!/usr/bin/env bash
set -euo pipefail

REMOTE="macserver"
REMOTE_DIR="/home/user/idealgram"

RSYNC_EXCLUDES=(
  --exclude=".git/"
  --exclude=".gradle/"
  --exclude="**/build/"
  --exclude="**/.cxx/"
  --exclude="**/.externalNativeBuild/"
  --exclude="**/.idea/"
  --exclude="**/*.iml"
  --exclude="apks/"
  --exclude="**/local.properties"
  --exclude="**/release.keystore"
)

MODE="${1:-default}"

case "$MODE" in
  default)
    NATIVE_TARGET="SKIP"
    ;;
  test)
    NATIVE_TARGET="SKIP"
    ;;
  native)
    NATIVE_TARGET="${2:-universal}"
    ;;
  *)
    exit 1
    ;;
esac

rsync -av --progress \
  "${RSYNC_EXCLUDES[@]}" \
  ./ \
  "$REMOTE:$REMOTE_DIR"

ssh "$REMOTE" bash <<EOF
set -euo pipefail
cd "$REMOTE_DIR"
export NATIVE_TARGET="$NATIVE_TARGET"
./gradlew assembleDebug --build-cache
EOF

mkdir -p ./apks
rsync -a --whole-file --inplace --no-compress --progress \
  -e "ssh -T -c aes128-gcm@openssh.com -o Compression=no" \
  "$REMOTE:$REMOTE_DIR/TMessagesProj/build/outputs/apk/debug/" \
  ./apks/
