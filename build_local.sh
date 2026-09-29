#!/usr/bin/env bash
# build_local.sh - AutoX.js 本地一键构建（v7 / v7_mini 签名 release）
# 用法：
#   ./build_local.sh            # 默认构建 v7
#   ./build_local.sh v7_mini    # 构建体积更小的 v7_mini
set -euo pipefail
cd "$(dirname "$0")"

FLAVOR="${1:-v7}"   # v7 | v7_mini

echo "AutoX.js 本地一键构建（flavor=$FLAVOR）"

# 0) 前置检查：node（autojs:buildJsModule 需要 Node 20+）
if ! command -v node >/dev/null 2>&1; then
  echo "错误：未找到 node，请安装 Node.js 20+（详见 README 编译相关章节）" >&2
  exit 1
fi
node_major=$(node -v | sed 's/^v//' | cut -d. -f1)
if [ "$node_major" -lt 20 ]; then
  echo "警告：Node 主版本 $node_major < 20，建议升级"
fi

# 1) 签名配置检查
ENV_READY=false
if [ "${CI:-}" = "true" ] && [ -n "${KEYSTORE_BASE64:-}" ]; then
  ENV_READY=true
elif [ -f "app/signing.properties" ]; then
  STORE_FILE=$(grep -E '^STORE_FILE=' app/signing.properties | head -1 | cut -d= -f2- || true)
  STORE_PASSWORD=$(grep -E '^STORE_PASSWORD=' app/signing.properties | head -1 | cut -d= -f2- || true)
  KEY_ALIAS=$(grep -E '^KEY_ALIAS=' app/signing.properties | head -1 | cut -d= -f2- || true)
  if [ -n "$STORE_FILE" ] && [ -f "$STORE_FILE" ] && [ -n "$STORE_PASSWORD" ] && [ -n "$KEY_ALIAS" ]; then
    ENV_READY=true
    echo "✓ 使用本地签名配置：app/signing.properties"
  else
    echo "警告：signing.properties 不完整或 keystore 缺失"
  fi
else
  echo "提示：未发现签名配置，将产出【未签名】release（无法安装）。如需签名："
  echo "  1) keytool -genkeypair -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias autox"
  echo "  2) 创建 app/signing.properties（字段见 README）"
fi

# 2) 构建（自动串联 :autojs:buildJsModule -> app:buildTemplateApp -> assemble<Variant>Release）
if [ "$FLAVOR" = "v7_mini" ]; then
  TASK="buildV7MiniReleaseLocal"
else
  TASK="buildV7ReleaseLocal"
fi
echo "▶ 构建 Gradle task：$TASK"
./gradlew "$TASK"

# 3) 汇总产物
OUT="app/build/outputs/apk/$FLAVOR/release"
if [ -d "$OUT" ]; then
  echo "✓ 产物目录：$OUT"
  ls -lh "$OUT"/*.apk 2>/dev/null || true
fi
