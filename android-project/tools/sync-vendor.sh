#!/usr/bin/env bash
# Baixa as arvores de codigo de terceiros usadas pelo build Android e aplica
# os patches do port. Rodar um UNICA vez antes de compilar:
#
#   android-project/tools/sync-vendor.sh
#
# Requisitos: git, curl e gcc (para gerar o m68kops do MAME 0.106).
set -euo pipefail
cd "$(dirname "$0")/../.."

ADV_REV=3693e29dcbce792549ef94f2704abea8f3febff1  # amadvance/advancemame (master (nov 2025))
SDL_TAG=release-2.30.11
MINIZ_TAG=3.1.2

# 1. AdvanceMAME (nucleo + advance + alien)
if [ ! -d advancemame ]; then
    git clone https://github.com/amadvance/advancemame.git advancemame
fi
git -C advancemame checkout -q "$ADV_REV"

# 2. miniz (gerado pelo cmake dele em builds normais; aqui usamos estatico via CMakeLists)
if [ ! -f advancemame/alien/deps/miniz-3.1.2/miniz.h ]; then
    mkdir -p /tmp/miniz advancemame/alien/deps
    curl -sL "https://codeload.github.com/richgel999/miniz/tar.gz/refs/tags/$MINIZ_TAG" -o /tmp/miniz/miniz.tar.gz
    tar -xzf /tmp/miniz/miniz.tar.gz -C /tmp/miniz
    rm -rf advancemame/alien/deps/miniz-$MINIZ_TAG
    mv /tmp/miniz/miniz-$MINIZ_TAG advancemame/alien/deps/miniz-$MINIZ_TAG
fi

# 3. miniz_export.h e gerado pelo cmake do miniz em builds normais
cat > advancemame/alien/deps/miniz-$MINIZ_TAG/miniz_export.h <<'EOF'
/* gerado para build estatico (o CMake do miniz gera este arquivo) */
#ifndef MINIZ_EXPORT_H
#define MINIZ_EXPORT_H
#define MINIZ_EXPORT
#endif
EOF

# 4. config.h da advance/lib (igual ao do autoconf original; o portable.h
#    inclui "config.h" a partir do proprio diretorio dele)
cp android-project/app/src/main/cpp/config/config.h advancemame/advance/lib/config.h

# 5. patch: clang nao calcula sqrt() em inicializador estatico (gcc calcula)
patch -d advancemame -p1 -N --dry-run -s < android-project/patches/vicdual-android.patch 2>/dev/null \
  && patch -d advancemame -p1 -N -s < android-project/patches/vicdual-android.patch \
  || echo "vicdual-android.patch ja aplicado"

# 6. gerar as tabelas do CPU 68000 (upstream gera com m68kmake em tempo de build)
gcc -O2 -o /tmp/m68kmake advancemame/mame/cpu/m68000/m68kmake.c
(cd advancemame/mame/cpu/m68000 && /tmp/m68kmake . m68k_in.c)

# 7. SDL2
if [ ! -d android-project/app/src/main/cpp/SDL2 ]; then
    curl -sL "https://github.com/libsdl-org/SDL/releases/download/$SDL_TAG/SDL2-2.30.11.tar.gz" -o /tmp/SDL2.tar.gz
    tar -xzf /tmp/SDL2.tar.gz -C /tmp
    mv /tmp/SDL2-2.30.11 android-project/app/src/main/cpp/SDL2
fi

echo "vendor sincronizado. Agora: cd android-project && gradle assembleDebug"
