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

# Fase 3: OSD SDL (src/osd) sobre o SDL2 vendored (app/src/main/cpp/SDL2).
file(GLOB_RECURSE HBM_OSD_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/osd/*.cpp")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/osd/windows/|/osd/mac/|/osd/winui/|/osd/sdl3/|/osd/asio\\.cpp$")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/debugger/win/|/debugger/qt/")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/modules/file/(win|winrt|stdfile)")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/modules/(midi/portmidi|sound/(pa|pulse|pipewire)_sound|output/network|opengl/gl_shader|lib/osdlib_(macosx|win32)|render/drawd3d|render/d3d/|lib/asio)")
# "SDL2/SDL.h": no CMake, o caminho SDL2/include do SDL vendored e exposto
# como <SDL2/...> por um link dentro da arvore de build.
file(MAKE_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim")
file(CREATE_LINK
    "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2/include"
    "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim/SDL2"
    SYMBOLIC)
add_library(hbmosd STATIC ${HBM_OSD_SOURCES})
target_include_directories(hbmosd PRIVATE
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
    "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim"
    "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2/include"
    "${HBM_DIR}/src/osd"
    "${HBM_DIR}/src/osd/sdl"
    "${HBM_DIR}/src/osd/modules/render"
    "${HBM_DIR}/src/frontend/mame"
    "${HBM_DIR}/3rdparty/bgfx/include"
    "${HBM_DIR}/3rdparty/bx/include"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/dear-imgui"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty"
    "${HBM_DIR}/3rdparty/bgfx/examples/common"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/khronos")
target_compile_definitions(hbmosd PRIVATE
    OSD_SDL USE_OPENGL=0 USE_BGFX=1 BX_CONFIG_DEBUG=0)
