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
PDIR="$(cd "$(dirname "$0")/../patches-hbm" && pwd)"

if [ ! -d hbmame ]; then
    rm -rf /tmp/hbm
    mkdir -p /tmp/hbm
    curl -fsSL "https://github.com/Robbbert/hbmame/archive/$HBM_REV.tar.gz" -o /tmp/hbm/h.tgz \
      || wget -qO /tmp/hbm/h.tgz "https://github.com/Robbbert/hbmame/archive/$HBM_REV.tar.gz"
    tar -xzf /tmp/hbm/h.tgz -C /tmp/hbm
    mv "/tmp/hbm/hbmame-$HBM_REV" hbmame
    for p in "$PDIR"/*.patch; do
        [ -e "$p" ] || continue
        git -C hbmame apply --whitespace=nowarn "$p" \
          || echo "(patch $(basename "$p") ja aplicado)"
    done
    # has_formats.h: o upstream gera com o GENie; geramos direto (equivale a
    # um #define HAS_FORMATS_<NOME> para cada formato compilado).
    rm -f hbmame/src/lib/formats/has_formats.h
    ( cd hbmame/src/lib/formats \
      && find . -maxdepth 1 -name '*.cpp' ! -name 'all.cpp' -printf '%f\n' \
      | sed 's/\.cpp$//' | tr '[:lower:]' '[:upper:]' | sort \
      > /tmp/.has_formats_names \
      && { echo '// ANDROID PORT: gerado no sync (equivale ao has_formats.h do GENie)'; \
           while read -r n; do echo "#define HAS_FORMATS_$n"; done \
           < /tmp/.has_formats_names; } > has_formats.h \
      && rm -f /tmp/.has_formats_names )
fi

echo "hbmame $HBM_REV pronto em ./hbmame (973 MB de fonte; 15.945 arquivos)"
