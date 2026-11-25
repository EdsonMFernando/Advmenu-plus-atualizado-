#!/usr/bin/env bash
# Baixa as arvores de codigo de terceiros usadas pelo build Android e aplica
# os patches do port. Rodar um UNICA vez antes de compilar:
#
#   android-project/tools/sync-vendor.sh
#
# Requisitos: git, curl e gcc (para gerar o m68kops do MAME 0.106).
set -euo pipefail
cd "$(dirname "$0")/../.."

ADV_REV=3693e29dcbce792549ef94f2704abea8f3febff1  # amadvance/advancemame (master, nov 2025)
SDL_TAG=release-2.30.11
MINIZ_TAG=3.1.2

apply_patch() {
    local f="$1"
    (cd advancemame && patch -p1 -N -s < "$OLDPWD/$f" 2>/dev/null) \
      || git -C advancemame apply --whitespace=nowarn "$OLDPWD/$f" 2>/dev/null \
      || echo "(patch $f ja aplicado)"
}

# 1. AdvanceMAME (nucleo + advance + alien)
if [ ! -d advancemame ]; then
    git clone -q https://github.com/amadvance/advancemame.git advancemame
fi
# busca o commit fixado exato (evita falha de arvores parciais em clones recentes)
git -C advancemame fetch -q --depth 1 origin "$ADV_REV" \
  && git -C advancemame checkout -q --detach "${ADV_REV}" \
  || git -C advancemame checkout -q "$ADV_REV"

# 2. miniz (gerado pelo cmake dele em builds normais; aqui usamos estatico via CMakeLists)
if [ ! -f advancemame/alien/deps/miniz-3.1.2/miniz.h ]; then
    rm -rf /tmp/miniz
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

# 5. patches do port Android
apply_patch android-project/patches/vicdual-android.patch
apply_patch android-project/patches/remove-megaman-art.patch

# 6. remove toda a arte custom do executavel (icone do Megaman e splash
#    do advmenu/advmame herdados do binario de Windows) e substitui o
#    icondef.dat (embutido na lib via advance/sdl/vsdl.c) por um icone
#    neutro totalmente transparente
rm -f advancemame/advance/lib/icondef.ico \
      advancemame/advance/lib/advmame_splash.bmp advancemame/advance/lib/advmame_alpha.bmp \
      advancemame/advance/lib/advmenu_splash.bmp advancemame/advance/lib/advmenu_alpha.bmp
cat > advancemame/advance/lib/icondef.dat <<'EOF'
/* icone neutro (totalmente transparente), substitui a arte custom removida */
static unsigned char icon_pixel[32*32] = {
	[0 ... 32*32-1] = 0x00
};

static unsigned char icon_mask[4*32] = {
	[0 ... 4*32-1] = 0xff
};

#define ICON_PALETTE 256

static unsigned char icon_palette[ICON_PALETTE*3] = {
	[0 ... ICON_PALETTE*3-1] = 0x00
};

#define ICON_SIZE 32
EOF

# 7. gerar as tabelas do CPU 68000 (upstream gera com m68kmake em tempo de build)
gcc -O2 -o /tmp/m68kmake advancemame/mame/cpu/m68000/m68kmake.c
(cd advancemame/mame/cpu/m68000 && /tmp/m68kmake . m68k_in.c)

# 8. SDL2
if [ ! -d android-project/app/src/main/cpp/SDL2 ]; then
    rm -f /tmp/SDL2.tar.gz
    curl -sL "https://github.com/libsdl-org/SDL/releases/download/$SDL_TAG/SDL2-2.30.11.tar.gz" -o /tmp/SDL2.tar.gz
    tar -xzf /tmp/SDL2.tar.gz -C /tmp
    mv /tmp/SDL2-2.30.11 android-project/app/src/main/cpp/SDL2
fi

echo "vendor sincronizado. Agora: cd android-project && gradle assembleDebug"
