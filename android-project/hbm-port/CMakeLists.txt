# PORT NATIVO HBMAME 0.289 (hbm-port) — Fase 2: nucleo via CMake (substitui o
# GENie). Compila as bibliotecas de suporte do nucleo MAME (util, formats,
# netlist) em .a estatico, para o NDK (arm64-v8a / armeabi-v7a). Nao afeta o
# build do APK enquanto o port nao estiver linkado (Fase 5); build manual:
#
#   cmake -G Ninja -B android-project/hbm-port/build/arm64-v8a \
#         -S android-project/hbm-port \
#         -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
#         -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-24
#   cmake --build android-project/hbm-port/build/arm64-v8a -t hbmcore
#
# Requisito: fonte do HBMAME vendored em hbmame/ (android-project/tools/
# sync-hbmame.sh, do PC durante o desenvolvimento; nada disso roda no APK).
cmake_minimum_required(VERSION 3.22)
project(hbm-port VERSION 0.289.0 LANGUAGES C CXX)

set(HBM_DIR "${CMAKE_CURRENT_LIST_DIR}/../../hbmame")
file(TO_CMAKE_PATH "${HBM_DIR}" HBM_DIR)

if(NOT EXISTS "${HBM_DIR}/src/lib/util/corestr.cpp")
    message(FATAL_ERROR
        "Fonte do HBMAME nao encontrada em ${HBM_DIR}. Baixe com "
        "android-project/tools/sync-hbmame.sh (do PC, uma vez).")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# CRLF=3: mesmo valor do smoke test; os drivers usam a infraestrutura de
# codigo do nucleo sem depender de features x86 do host.
add_compile_definitions(CRLF=3)

include_directories(
    "${HBM_DIR}/src/lib"
    "${HBM_DIR}/src/lib/netlist"
    "${HBM_DIR}/src/lib/netlist/core"
    "${HBM_DIR}/src/lib/netlist/devices"
    "${HBM_DIR}/src/lib/netlist/prg"
    "${HBM_DIR}/src/lib/util"
    "${HBM_DIR}/src/lib/formats"
    "${HBM_DIR}/src/osd"
    "${HBM_DIR}/src/devices"
    "${HBM_DIR}/src/emu"
    "${HBM_DIR}/3rdparty"
    "${HBM_DIR}/3rdparty/flac/include"
    "${HBM_DIR}/3rdparty/zstd/lib"
    "${HBM_DIR}/3rdparty/expat/lib"
    "${HBM_DIR}/3rdparty/utf8proc"
    "${HBM_DIR}/3rdparty/nanosvg/src"
    "${HBM_DIR}/3rdparty/lzma/C"
    "${HBM_DIR}/3rdparty/aes128cbc"
    "${HBM_DIR}/3rdparty/aes192cbc"
    "${HBM_DIR}/3rdparty/aes256cbc"
    "${HBM_DIR}/3rdparty/libjpeg"
    "${HBM_DIR}/3rdparty/expat"
    "${HBM_DIR}/3rdparty/pugixml"
    "${HBM_DIR}/3rdparty/softfloat3/include"
    "${HBM_DIR}/3rdparty/glm"
    "${HBM_DIR}/3rdparty/rapidjson/include"
    "${HBM_DIR}/3rdparty/lua"
    "${HBM_DIR}/3rdparty/zlib"
)

file(GLOB_RECURSE HBM_UTIL_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/lib/util/*.cpp")
file(GLOB HBM_FORMATS_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/lib/formats/*.cpp")
file(GLOB_RECURSE HBM_NETLIST_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/lib/netlist/*.cpp")
list(FILTER HBM_NETLIST_SOURCES EXCLUDE REGEX
    "/examples/|/tests/|/prg/")

# has_formats.h: o upstream gera pelo GENie; geramos direto no CMake
# (equivale a #define HAS_FORMATS_<NOME> para cada formato compilado),
# dentro da arvore de build — sem tocar na fonte vendored.
set(HAS_FORMATS_H "${CMAKE_CURRENT_BINARY_DIR}/generated/has_formats.h")
set(HAS_FORMATS_CONTENT "")
foreach(src ${HBM_FORMATS_SOURCES})
    get_filename_component(name "${src}" NAME_WE)
    string(TOUPPER "${name}" name)
    string(REGEX REPLACE "[^A-Z0-9]" "_" name "${name}")
    string(APPEND HAS_FORMATS_CONTENT "#define HAS_FORMATS_${name} 1\n")
endforeach()
file(WRITE "${HAS_FORMATS_H}" "${HAS_FORMATS_CONTENT}")

add_library(hbmcore STATIC
    ${HBM_UTIL_SOURCES} ${HBM_FORMATS_SOURCES} ${HBM_NETLIST_SOURCES})
target_include_directories(hbmcore PRIVATE
    "${CMAKE_CURRENT_BINARY_DIR}/generated")
