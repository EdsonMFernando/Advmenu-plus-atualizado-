#!/usr/bin/env bash
# Baixa o codigo-fonte do HBMAME (Robbbert) fixado no commit do release 0.289.6
# para o projeto de port nativo Android (hbm-port/). Idempotente; NAO e rodado
# pelo build do APK — comeca a ser usado quando o port nativo (hbm-port) estiver
# compilando. Uso manual:
#
#   android-project/tools/sync-hbmame.sh
set -euo pipefail
cd "$(dirname "$0")/../.."

HBM_REV=fa040eced0dea161bb4e5d55bdbcb85aaf8de632  # hbmame 0.289.6 ("Files for 289.6")

if [ ! -d hbmame ]; then
    rm -rf /tmp/hbm
    mkdir -p /tmp/hbm
    curl -fsSL "https://github.com/Robbbert/hbmame/archive/$HBM_REV.tar.gz" -o /tmp/hbm/h.tgz \
      || wget -qO /tmp/hbm/h.tgz "https://github.com/Robbbert/hbmame/archive/$HBM_REV.tar.gz"
    tar -xzf /tmp/hbm/h.tgz -C /tmp/hbm
    mv "/tmp/hbm/hbmame-$HBM_REV" hbmame
fi

echo "hbmame $HBM_REV pronto em ./hbmame (973 MB de fonte; 15.945 arquivos)"
