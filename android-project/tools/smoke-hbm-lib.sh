#!/usr/bin/env bash
# Smoke test da Fase 2 do port nativo HBMAME 0.289 (hbm-port/): compila as
# bibliotecas do nucleo com o clang do NDK e empacota em .a estatico.
# Idempotente; NAO e rodado pelo build do APK. Uso manual:
#
#   android-project/tools/smoke-hbm-lib.sh [arm64-v8a|armeabi-v7a]
#
# Requisito: fonte do HBMAME ja sincronizada (android-project/tools/sync-hbmame.sh)
# e o NDK 26 em $ANDROID_NDK_HOME ou /tmp/tools/android-ndk-r26d.
set -uo pipefail
cd "$(dirname "$0")/../.."

ABI="${1:-arm64-v8a}"
NDK="${ANDROID_NDK_HOME:-/tmp/tools/android-ndk-r26d}"
PRE="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
case "$ABI" in
    arm64-v8a)  CC="$PRE/aarch64-linux-android24-clang++"; AR="$PRE/llvm-ar" ;;
    armeabi-v7a) CC="$PRE/armv7a-linux-androideabi24-clang++"; AR="$PRE/llvm-ar" ;;
    *) echo "abi desconhecida: $ABI"; exit 1 ;;
esac
[ -x "$CC" ] || { echo "NDK nao encontrado em $NDK"; exit 1; }

OUT="$(pwd)/android-project/hbm-port/build-smoke/$ABI"
mkdir -p "$OUT"
ERR="$OUT/erros.log"; : > "$ERR"

CFLAGS="-std=c++20 -O1 -DCRLF=3 -Isrc/lib -Isrc/lib/netlist -Isrc/lib/netlist/core \
-Isrc/lib/netlist/devices -Isrc/lib/netlist/prg -Isrc/lib/util -Isrc/lib/formats \
-Isrc/osd -Isrc/devices -Isrc/emu -I3rdparty -I3rdparty/flac/include -I3rdparty/zstd/lib -I3rdparty/expat/lib \
-I3rdparty/utf8proc -I3rdparty/nanosvg/src -I3rdparty/lzma/C \
-I3rdparty/aes128cbc -I3rdparty/aes192cbc -I3rdparty/aes256cbc \
-I3rdparty/libjpeg -I3rdparty/expat -I3rdparty/pugixml \
-I3rdparty/softfloat3/include -I3rdparty/glm -I3rdparty/rapidjson/include \
-I3rdparty/lua -I3rdparty/zlib"

# Grupo do nucleo: examples/ sao demos standalone fora da biblioteca; tests/ e
# prg/ (nltool/nlwav) compilam mas nao entram no .a. Cada grupo gera <grupo>.objs.
cat > "$OUT/compile-one.sh" <<HELP
#!/bin/sh
cd hbmame
f="\$1"; g="\$2"; f="\${f#hbmame/}"
o="$OUT/\$(echo "\$f" | tr '/' '_').o"
if [ -e "\$o" ]; then echo "\$o" >> "$OUT/\$g.objs"; exit 0; fi
"$CC" $CFLAGS -c "\$f" -o "\$o" 2>>"$ERR" || { echo "FAIL \$f"; exit 0; }
echo "\$o" >> "$OUT/\$g.objs"
HELP
chmod +x "$OUT/compile-one.sh"

for g in util formats netlist; do
    : > "$OUT/$g.objs"
done
find hbmame/src/lib/util -name '*.cpp' | xargs -P "$(nproc)" -I{} "$OUT/compile-one.sh" {} util
find hbmame/src/lib/formats -name '*.cpp' | xargs -P "$(nproc)" -I{} "$OUT/compile-one.sh" {} formats
find hbmame/src/lib/netlist -name '*.cpp' ! -path '*/examples/*' ! -path '*/tests/*' \
  | xargs -P "$(nproc)" -I{} "$OUT/compile-one.sh" {} netlist

rm -f "$OUT/libhbmcore-$ABI.a"
"$AR" rcs "$OUT/libhbmcore-$ABI.a" $(cat "$OUT"/*.objs | tr '\n' ' ')
echo "util=$(wc -l < "$OUT/util.objs") formats=$(wc -l < "$OUT/formats.objs") netlist=$(wc -l < "$OUT/netlist.objs")"
ls -la "$OUT/libhbmcore-$ABI.a"
grep 'error:' "$ERR" | head -5 || true
