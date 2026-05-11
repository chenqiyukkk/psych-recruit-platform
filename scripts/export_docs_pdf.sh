#!/usr/bin/env bash
set -euo pipefail

# Export Markdown docs under docs/reports/ to PDF.
# Requires Docker installed locally.
# Outputs PDFs into docs/reports/.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ARCH_MD="docs/reports/组长-架构设计文档.md"
OPS_MD="docs/reports/组长-软件配置与运维文档.md"

ARCH_PDF="docs/reports/组长-架构设计文档.pdf"
OPS_PDF="docs/reports/组长-软件配置与运维文档.pdf"

docker run --rm -v "$ROOT_DIR:/data" --workdir /data pandoc/latex:3.1.13 \
  "$ARCH_MD" -o "$ARCH_PDF"

docker run --rm -v "$ROOT_DIR:/data" --workdir /data pandoc/latex:3.1.13 \
  "$OPS_MD" -o "$OPS_PDF"

echo "Generated:"
echo " - $ARCH_PDF"
echo " - $OPS_PDF"

