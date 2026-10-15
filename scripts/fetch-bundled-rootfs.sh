#!/usr/bin/env bash
# 生成 App 内置的 Agent 预终端文件（Ubuntu 24.04 noble arm64 rootfs）。
#
# 华为云的 noble-server-cloudimg-arm64.tar.gz 内是 ext4 磁盘镜像而非 rootfs
# 文件树，本脚本将其中的根文件系统提取出来打包成 assets 可直接解压的格式。
#
# 依赖: e2fsprogs (debugfs)、pigz（可选，缺省时回退 gzip）
set -euo pipefail

URL="https://repo.huaweicloud.com/ubuntu-cloud-images/noble/current/noble-server-cloudimg-arm64.tar.gz"
OUT="app/src/main/assets/lab/noble-rootfs-arm64.tgz"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

command -v debugfs >/dev/null || { echo "缺少 debugfs，请先安装 e2fsprogs" >&2; exit 1; }

echo "==> 下载 Ubuntu noble server cloud image (arm64)"
curl -fL --retry 3 -o "$WORK/cloudimg.tar.gz" "$URL"

echo "==> 解出 ext4 磁盘镜像"
tar -xzf "$WORK/cloudimg.tar.gz" -C "$WORK" noble-server-cloudimg-arm64.img

echo "==> 提取根文件系统 (debugfs rdump)"
mkdir -p "$WORK/rootfs"
debugfs -R "rdump / $WORK/rootfs" "$WORK/noble-server-cloudimg-arm64.img" 2>/dev/null

echo "==> 清理无用数据"
rm -rf "$WORK/rootfs/lost+found" "$WORK/rootfs/swapfile"
find "$WORK/rootfs/var/cache/apt/archives" -name '*.deb' -delete 2>/dev/null || true

echo "==> 打包为 $OUT"
mkdir -p "$(dirname "$OUT")"
if command -v pigz >/dev/null; then
  tar -cf - -C "$WORK/rootfs" --numeric-owner ./ | pigz -6 -p "$(nproc)" > "$OUT"
else
  tar -czf "$OUT" -C "$WORK/rootfs" --numeric-owner ./
fi

echo "==> 完成: $OUT ($(du -h "$OUT" | cut -f1)), $(tar -tzf "$OUT" | wc -l) 个条目"
